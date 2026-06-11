Feature: Spending Summary
  As an authenticated user
  I want to see a monthly spending summary grouped by category
  So that I know how much I spend on subscriptions each month

  Background:
    Given a registered user with username "spenduser", email "spenduser@example.com" and password "pass1234"
    And the user is authenticated as "spenduser" with password "pass1234"
    And the category "Entertainment" already exists

  Scenario: Monthly subscription appears at its original price in the summary
    Given the user has an active subscription named "Netflix" with price "15.99", billing cycle "MONTHLY", currency "EUR" in category "Entertainment"
    When the user requests the spending summary
    Then the response status should be 200
    And the summary should contain a subscription named "Netflix" with monthly price "15.99"

  Scenario: Annual subscription is normalised to a monthly price in the summary
    Given the user has an active subscription named "Spotify Annual" with price "99.00", billing cycle "ANNUAL", currency "EUR" in category "Entertainment"
    When the user requests the spending summary
    Then the response status should be 200
    And the summary should contain a subscription named "Spotify Annual" with monthly price "8.25"

  Scenario: Quarterly subscription is normalised to a monthly price in the summary
    Given the user has an active subscription named "QuarterlyApp" with price "30.00", billing cycle "QUARTERLY", currency "EUR" in category "Entertainment"
    When the user requests the spending summary
    Then the response status should be 200
    And the summary should contain a subscription named "QuarterlyApp" with monthly price "10.00"

  Scenario: Inactive subscriptions are excluded from the spending summary
    Given the user has an inactive subscription named "OldService" with price "50.00", billing cycle "MONTHLY", currency "EUR" in category "Entertainment"
    When the user requests the spending summary
    Then the response status should be 200
    And the summary should not contain a subscription named "OldService"
