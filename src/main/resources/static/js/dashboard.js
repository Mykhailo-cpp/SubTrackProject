/* SubTrack — dashboard page controller.
 *
 * Boot note: layout.js fires "layout:ready" synchronously inside
 * DOMContentLoaded. All scripts are defer-ordered, so by the time the browser
 * executes dashboard.js the event has already fired. We call init() directly
 * and also keep the listener as a belt-and-suspenders fallback.
 */
(function () {

  /* ── Palette ────────────────────────────────────────────────────────────── */
  var PALETTE = [
    "#0f766e","#e2683c","#7c3aed","#0ea5e9","#d97706",
    "#db2777","#16a34a","#9333ea","#ca8a04","#0891b2",
    "#dc2626","#4f46e5",
  ];

  /* ── State ──────────────────────────────────────────────────────────────── */
  var summaryData      = null;
  var allSubs          = [];
  var chartInstance    = null;
  var selectedCurrency = "";
  var conversionCache  = {};   // "subId:CURRENCY" → Number (exchangeRate)
  var rateCache        = {};   // "name|origCurrency:targetCurrency" → Number (exchangeRate) for summary subs
  var booted           = false;

  /* ── Boot ───────────────────────────────────────────────────────────────── */
  function init() {
    if (booted) return;   // guard: don't run twice if both paths fire
    booted = true;

    loadDashboard();

    var sel = document.getElementById("currencySelect");
    if (sel) {
      sel.addEventListener("change", function () {
        selectedCurrency = sel.value.toUpperCase();
        onCurrencyChange();
      });
    }
  }

  /* layout:ready fires before this script runs (defer order), so the listener
     below is the primary path. The readyState check is a safety net. */
  document.addEventListener("layout:ready", init);
  if (document.readyState !== "loading" && document.getElementById("dashLoading")) {
    init();
  }

  /* ── Data loading ───────────────────────────────────────────────────────── */
  async function loadDashboard() {
    showState("loading");
    try {
      var results = await Promise.all([
        Api.get("/subscriptions/summary"),
        Api.get("/subscriptions"),
      ]);
      summaryData = results[0];
      allSubs     = Api.unwrapCollection(results[1], "subscriptionResponseList");

      var activeCount = (summaryData.categories || []).reduce(function (n, cat) {
        return n + (cat.subscriptions || []).length;
      }, 0);

      if (activeCount === 0) { showState("empty"); return; }
      showState("content");
      render();
    } catch (err) {
      if (err && err.status === 401) return; // api.js already redirected
      showState("error", err.message || "Couldn't load your subscriptions. Please try refreshing.");
    }
  }

  /* ── Master render ──────────────────────────────────────────────────────── */
  function render() {
    renderChart();
    renderSpendCards();
    renderRenewals();
  }

  /* ── Doughnut chart ─────────────────────────────────────────────────────── */
  function renderChart() {
    var categories = summaryData.categories || [];
    var labels = [], amounts = [], colors = [], legendItems = [];

    categories.forEach(function (cat, i) {
      var subs = cat.subscriptions || [];
      var total = 0, currency = null, mixed = false;

      subs.forEach(function (sub) {
        var price = Number(sub.monthlyPrice) || 0;
        var cur   = sub.currency || "";

        if (selectedCurrency && selectedCurrency !== cur) {
          var rate = conversionCache[sub.id + ":" + selectedCurrency]
                  || rateCache[(sub.name || "") + "|" + cur + ":" + selectedCurrency];
          if (rate != null) {
            price = (Number(sub.monthlyPrice) || 0) * rate;
            cur = selectedCurrency;
          }
        } else if (selectedCurrency && selectedCurrency === cur) {
          cur = selectedCurrency;
        }

        if (currency === null) { currency = cur; }
        else if (currency !== cur) { mixed = true; }
        total += price;
      });

      labels.push(cat.categoryName || "Uncategorised");
      amounts.push(total);
      colors.push(PALETTE[i % PALETTE.length]);
      legendItems.push({ name: cat.categoryName || "Uncategorised", amount: total, currency: mixed ? "" : (currency || ""), color: PALETTE[i % PALETTE.length] });
    });

    var grandTotal = amounts.reduce(function (s, v) { return s + v; }, 0);

    var centreEl = document.getElementById("chartCentreTotal");
    var noteEl   = document.getElementById("chartCurrencyNote");
    if (centreEl) {
      var seenCurs = {};
      categories.forEach(function (cat) {
        (cat.subscriptions || []).forEach(function (s) { seenCurs[s.currency] = true; });
      });
      var dispCur = selectedCurrency || (Object.keys(seenCurs).length === 1 ? Object.keys(seenCurs)[0] : "");
      centreEl.textContent = dispCur ? formatMoney(grandTotal, dispCur) : grandTotal.toFixed(2);
    }
    if (noteEl) {
      noteEl.textContent = selectedCurrency ? "Converted · " + selectedCurrency : "Multiple currencies";
    }

    var canvas = document.getElementById("spendChart");
    if (!canvas) return;

    if (chartInstance) {
      chartInstance.data.labels                      = labels;
      chartInstance.data.datasets[0].data            = amounts;
      chartInstance.data.datasets[0].backgroundColor = colors;
      chartInstance.update();
    } else {
      chartInstance = new Chart(canvas, {
        type: "doughnut",
        data: { labels: labels, datasets: [{ data: amounts, backgroundColor: colors, borderWidth: 2, borderColor: "#ffffff", hoverBorderWidth: 3 }] },
        options: {
          cutout: "68%",
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                label: function (ctx) {
                  var item = legendItems[ctx.dataIndex];
                  if (!item) return ctx.label;
                  var pct = grandTotal > 0 ? ((item.amount / grandTotal) * 100).toFixed(1) : "0.0";
                  var amtStr = item.currency ? formatMoney(item.amount, item.currency) : item.amount.toFixed(2);
                  return " " + amtStr + "/mo  (" + pct + "%)";
                },
              },
            },
          },
          animation: { duration: 400 },
        },
      });
    }

    var legendEl = document.getElementById("chartLegend");
    if (!legendEl) return;
    legendEl.innerHTML = legendItems.map(function (item) {
      var amtStr = item.currency ? formatMoney(item.amount, item.currency) + "/mo" : item.amount.toFixed(2) + "/mo";
      return "<li><span class='legend-dot' style='background:" + item.color + "' aria-hidden='true'></span><span class='legend-name'>" + escHtml(item.name) + "</span><span class='legend-amount'>" + escHtml(amtStr) + "</span></li>";
    }).join("");
  }

  /* ── Spend summary cards ────────────────────────────────────────────────── */
  function renderSpendCards() {
    var container = document.getElementById("spendCards");
    if (!container) return;

    var totals = {};
    (summaryData.categories || []).forEach(function (cat) {
      (cat.subscriptions || []).forEach(function (sub) {
        var cur = sub.currency || "?";
        if (!totals[cur]) totals[cur] = { monthly: 0, yearly: 0 };
        totals[cur].monthly += Number(sub.monthlyPrice) || 0;
        totals[cur].yearly  += Number(sub.yearlyPrice)  || 0;
      });
    });

    var currencies = Object.keys(totals);
    if (!currencies.length) {
      container.innerHTML = "<p style='color:var(--st-muted);font-size:.88rem;'>No spend data.</p>";
      return;
    }

    container.innerHTML = currencies.map(function (cur) {
      var t = totals[cur];
      var cM = 0, cY = 0, allOk = true;

      if (selectedCurrency && selectedCurrency !== cur) {
        (summaryData.categories || []).forEach(function (cat) {
          (cat.subscriptions || []).forEach(function (sub) {
            if (sub.currency !== cur) return;
            var rate = conversionCache[sub.id + ":" + selectedCurrency]
                    || rateCache[(sub.name || "") + "|" + sub.currency + ":" + selectedCurrency];
            if (rate != null) {
              cM += (Number(sub.monthlyPrice) || 0) * rate;
              cY += (Number(sub.yearlyPrice)  || 0) * rate;
            } else { allOk = false; }
          });
        });
      } else { allOk = false; }

      var convBlock = (allOk && selectedCurrency && selectedCurrency !== cur) ? (
        "<div class='sci-row'><span class='sci-period'>Monthly ≈</span><span class='sci-converted'>" + escHtml(formatMoney(cM, selectedCurrency)) + "</span></div>" +
        "<div class='sci-row'><span class='sci-period'>Yearly ≈</span><span class='sci-converted'>"  + escHtml(formatMoney(cY, selectedCurrency)) + "</span></div>"
      ) : "";

      return (
        "<div class='spend-card-item'>" +
          "<div class='sci-currency'>" + escHtml(cur) + "</div>" +
          "<div class='sci-row'><span class='sci-period'>Monthly</span><span class='sci-amount'>" + escHtml(formatMoney(t.monthly, cur)) + "</span></div>" +
          "<div class='sci-row'><span class='sci-period'>Yearly</span><span class='sci-amount'>"  + escHtml(formatMoney(t.yearly,  cur)) + "</span></div>" +
          convBlock +
        "</div>"
      );
    }).join("");
  }

  /* ── Upcoming renewals ──────────────────────────────────────────────────── */
  function renderRenewals() {
    var listEl  = document.getElementById("renewalsList");
    var emptyEl = document.getElementById("renewalsEmpty");
    var badgeEl = document.getElementById("renewalsBadge");
    if (!listEl) return;

    var today  = todayDate();
    var cutoff = new Date(today);
    cutoff.setDate(cutoff.getDate() + 30);

    var upcoming = allSubs.filter(function (sub) {
      if (!sub.active || !sub.nextRenewalDate) return false;
      var d = parseDate(sub.nextRenewalDate);
      return d >= today && d <= cutoff;
    }).sort(function (a, b) { return parseDate(a.nextRenewalDate) - parseDate(b.nextRenewalDate); });

    if (badgeEl) badgeEl.textContent = upcoming.length ? upcoming.length + " due" : "None due";

    if (!upcoming.length) {
      if (emptyEl) emptyEl.classList.remove("d-none");
      listEl.innerHTML = "";
      return;
    }
    if (emptyEl) emptyEl.classList.add("d-none");

    listEl.innerHTML = upcoming.map(function (sub) {
      var renewDate = parseDate(sub.nextRenewalDate);
      var daysLeft  = Math.round((renewDate - today) / 86400000);
      var isToday   = daysLeft === 0;

      var daysBox = isToday
        ? "<div class='renewal-days is-today'><span class='rd-num'>!</span><span class='rd-unit'>today</span></div>"
        : "<div class='renewal-days'><span class='rd-num'>" + daysLeft + "</span><span class='rd-unit'>day" + (daysLeft === 1 ? "" : "s") + "</span></div>";

      var dateStr = renewDate.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });

      var convertedStr = "";
      if (selectedCurrency && selectedCurrency !== sub.currency) {
        var rate = conversionCache[sub.id + ":" + selectedCurrency];
        if (rate != null) {
          var convertedAmt = (Number(sub.price) || 0) * rate;
          convertedStr = "<span class='rp-converted'>≈ " + escHtml(formatMoney(convertedAmt, selectedCurrency)) + "</span>";
        }
      }

      return (
        "<div class='renewal-row'>" + daysBox +
          "<div class='renewal-info'>" +
            "<div class='renewal-name'>" + escHtml(sub.name) + "</div>" +
            "<div class='renewal-meta'>" + escHtml(dateStr) + " · " + escHtml(formatCycle(sub.billingCycle)) + (sub.categoryName ? " · " + escHtml(sub.categoryName) : "") + "</div>" +
          "</div>" +
          "<div class='renewal-price'>" + escHtml(formatMoney(Number(sub.price) || 0, sub.currency || "")) + convertedStr + "</div>" +
        "</div>"
      );
    }).join("");
  }

  /* ── Currency conversion ────────────────────────────────────────────────── */
  async function onCurrencyChange() {
    hideCurrencyError();

    if (!selectedCurrency) {
      conversionCache = {};
      rateCache = {};
      render();
      return;
    }

    /* Subscriptions that need a fresh conversion (not cached yet) */
    var toConvert = allSubs.filter(function (sub) {
      return sub.active &&
             sub.id != null &&
             sub.currency !== selectedCurrency &&
             conversionCache[sub.id + ":" + selectedCurrency] == null;
    });

    if (!toConvert.length) { render(); return; }

    setConverting(true);

    /* Fire all conversions in parallel and wait for ALL to settle.
     * We never await a bare Api.get here — every individual call is wrapped in
     * its own try/catch so a 4xx/5xx from the exchange-rate service cannot
     * propagate to the outer scope and cannot be mistaken for a 401 by api.js.
     *
     * api.js only calls Auth.clearSession()+redirect when IT receives a 401.
     * That can't happen here because we catch every rejection below. */
    var failCount = 0;
    var results   = await Promise.allSettled(
      toConvert.map(function (sub) {
        return convertOne(sub);
      })
    );

    results.forEach(function (r) {
      if (r.status === "rejected") failCount++;
    });

    setConverting(false);

    if (failCount > 0 && failCount === toConvert.length) {
      /* Every request failed — most likely the exchange-rate API key isn't set */
      showCurrencyError(
        "Currency conversion is unavailable right now. " +
        "Check that the EXCHANGERATE_API_KEY is configured on the server."
      );
    } else if (failCount > 0) {
      showCurrencyError("Some prices couldn't be converted and show their original currency.");
    }

    render();
  }

  /* Converts a single subscription and stores the result in the cache.
   * Returns a resolved promise on success OR failure — never rejects. */
  async function convertOne(sub) {
    try {
      var result = await rawConvert(sub.id, selectedCurrency);
      /* CurrencyConversionResponse: { originalPrice, originalCurrency,
                                        convertedPrice, targetCurrency, exchangeRate } */
      var rate = Number(result.exchangeRate);
      conversionCache[sub.id + ":" + selectedCurrency] = rate;
      rateCache[sub.name + "|" + sub.currency + ":" + selectedCurrency] = rate;
    } catch (err) {
      /* Swallow — failure is tracked by Promise.allSettled status */
      throw err; // re-throw so allSettled marks it "rejected"
    }
  }

  /* Raw fetch for one conversion — isolated so api.js's 401 handler cannot
   * reach the outer onCurrencyChange flow. If the server returns 401 here,
   * api.js will redirect. That's intentional — the session really is gone. */
  function rawConvert(subId, currency) {
    return Api.get("/subscriptions/" + subId + "/convert?targetCurrency=" + encodeURIComponent(currency));
  }

  function setConverting(active) {
    var el = document.getElementById("currencyConverting");
    if (el) el.classList.toggle("visible", active);
  }

  function showCurrencyError(msg) {
    var el = document.getElementById("currencyError");
    if (!el) return;
    el.textContent = msg;
    el.classList.remove("d-none");
  }

  function hideCurrencyError() {
    var el = document.getElementById("currencyError");
    if (el) el.classList.add("d-none");
  }

  /* ── UI state machine ───────────────────────────────────────────────────── */
  function showState(state, msg) {
    ["dashLoading","dashError","dashEmpty","dashContent"].forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.classList.add("d-none");
    });
    if (state === "loading") {
      show("dashLoading");
    } else if (state === "error") {
      var el = document.getElementById("dashError");
      if (el) { el.textContent = msg || "Something went wrong."; el.classList.remove("d-none"); }
    } else if (state === "empty") {
      show("dashEmpty");
    } else if (state === "content") {
      show("dashContent");
    }
  }

  function show(id) { var el = document.getElementById(id); if (el) el.classList.remove("d-none"); }

  /* ── Utilities ──────────────────────────────────────────────────────────── */
  function formatMoney(amount, currency) {
    if (!currency) return Number(amount).toFixed(2);
    try {
      return new Intl.NumberFormat(undefined, { style: "currency", currency: currency, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount);
    } catch (e) { return currency + "\u00a0" + Number(amount).toFixed(2); }
  }

  function formatCycle(cycle) {
    return ({ MONTHLY:"Monthly", QUARTERLY:"Quarterly", SEMI_ANNUAL:"Semi-annual", ANNUAL:"Annual", YEARLY:"Yearly", WEEKLY:"Weekly" })[cycle]
      || (cycle ? cycle.charAt(0) + cycle.slice(1).toLowerCase() : "");
  }

  function parseDate(str) {
    if (!str) return new Date(NaN);
    var p = str.split("-");
    return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
  }

  function todayDate() { var d = new Date(); return new Date(d.getFullYear(), d.getMonth(), d.getDate()); }

  function escHtml(s) {
    return String(s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
  }

})();