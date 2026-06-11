Feature: User Authentication
  As a new or returning user
  I want to register and log in to SubTrack
  So that I can securely manage my subscriptions

  Scenario: Successful user registration
    When a user registers with username "alice", email "alice@example.com" and password "Secret@123"
    Then the response status should be 200
    And the response body should contain a JWT token
    And the response body should contain the username "alice"

  Scenario: Duplicate username is rejected on registration
    Given a registered user with username "bob", email "bob@example.com" and password "Secret@123"
    When a user registers with username "bob", email "bob2@example.com" and password "Pass@4567"
    Then the response status should be 409

  Scenario: Successful login with valid credentials
    Given a registered user with username "carol", email "carol@example.com" and password "MyP@ssword1"
    When the user logs in with username "carol" and password "MyP@ssword1"
    Then the response status should be 200
    And the response body should contain a JWT token
    And the response body should contain the username "carol"

  Scenario: Login is rejected for a non-existent user
    When the user logs in with username "ghost" and password "NoP@ssword1"
    Then the response status should be 401

  Scenario: Login is rejected for wrong password
    Given a registered user with username "dave", email "dave@example.com" and password "Correct@1!"
    When the user logs in with username "dave" and password "Wrong@1234"
    Then the response status should be 401

  Scenario: Forgot password sends reset email for known address
    Given a registered user with username "eve", email "eve@example.com" and password "Eve@secret1!"
    When the user requests a password reset for email "eve@example.com"
    Then the response status should be 200

  Scenario: Forgot password returns 200 even for unknown email
    When the user requests a password reset for email "nobody@example.com"
    Then the response status should be 200

  Scenario: Weak password is rejected on registration
    When a user registers with username "weakuser", email "weak@example.com" and password "short"
    Then the response status should be 400

  Scenario: Password without special character is rejected on registration
    When a user registers with username "nospecial", email "nospecial@example.com" and password "Password123"
    Then the response status should be 400