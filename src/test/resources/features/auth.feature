Feature: User Authentication
  As a new or returning user
  I want to register and log in to SubTrack
  So that I can securely manage my subscriptions

  Scenario: Successful user registration
    When a user registers with username "alice", email "alice@example.com" and password "secret123"
    Then the response status should be 200
    And the response body should contain a JWT token
    And the response body should contain the username "alice"

  Scenario: Duplicate username is rejected on registration
    Given a registered user with username "bob", email "bob@example.com" and password "secret123"
    When a user registers with username "bob", email "bob2@example.com" and password "pass456"
    Then the response status should be 409

  Scenario: Successful login with valid credentials
    Given a registered user with username "carol", email "carol@example.com" and password "mypassword"
    When the user logs in with username "carol" and password "mypassword"
    Then the response status should be 200
    And the response body should contain a JWT token
    And the response body should contain the username "carol"

  Scenario: Login is rejected for a non-existent user
    When the user logs in with username "ghost" and password "nopassword"
    Then the response status should be 401

  Scenario: Login is rejected for wrong password
    Given a registered user with username "dave", email "dave@example.com" and password "correct"
    When the user logs in with username "dave" and password "wrong"
    Then the response status should be 401
