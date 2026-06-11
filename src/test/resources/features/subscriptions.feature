Feature: Subscription Management
  As an authenticated user
  I want to create, view, update and delete my subscriptions
  So that I can keep an accurate record of everything I pay for

  Background:
    Given a registered user with username "subuser", email "subuser@example.com" and password "pass1234"
    And the user is authenticated as "subuser" with password "pass1234"
    And the category "Software" already exists

  Scenario: Successfully create a new subscription
    When the user creates a subscription with the following details:
      | name           | price | currency | billingCycle | nextRenewalDate | active | renewalReminderEnabled | categoryName |
      | GitHub Copilot | 10.00 | USD      | MONTHLY      | 2026-07-01      | true   | true                   | Software     |
    Then the response status should be 201
    And the response body should contain the subscription name "GitHub Copilot"

  Scenario: List all subscriptions for the authenticated user
    When the user creates a subscription with the following details:
      | name      | price | currency | billingCycle | nextRenewalDate | active | renewalReminderEnabled | categoryName |
      | JetBrains | 24.90 | EUR      | MONTHLY      | 2026-07-15      | true   | false                  | Software     |
    And the user requests all their subscriptions
    Then the response status should be 200
    And the subscription list should contain "JetBrains"

  Scenario: Successfully update an existing subscription
    Given the user has a subscription named "OldName" with price "5.00" and billing cycle "MONTHLY" in category "Software"
    When the user updates that subscription with name "NewName" and price "9.99"
    Then the response status should be 200
    And the response body should contain the subscription name "NewName"

  Scenario: Successfully delete a subscription
    Given the user has a subscription named "ToDelete" with price "3.00" and billing cycle "MONTHLY" in category "Software"
    When the user deletes that subscription
    Then the response status should be 204

  Scenario: Getting a subscription that does not exist returns 404
    When the user requests subscription with id 99999
    Then the response status should be 404

  Scenario: Another user cannot access a subscription they do not own
    Given a registered user with username "otheruser", email "other@example.com" and password "pass1234"
    And the user is authenticated as "otheruser" with password "pass1234"
    And the user has a subscription named "Private Sub" with price "8.00" and billing cycle "MONTHLY" in category "Software"
    And the user is authenticated as "subuser" with password "pass1234"
    When the user requests the subscription owned by "otheruser" named "Private Sub"
    Then the response status should be 404