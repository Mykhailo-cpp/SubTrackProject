Feature: Category Management
  As an authenticated user
  I want to create and list subscription categories
  So that I can organise my subscriptions into meaningful groups

  Background:
    Given a registered user with username "catuser", email "catuser@example.com" and password "P@ssword1!"
    And the user is authenticated as "catuser" with password "P@ssword1!"

  Scenario: Successfully create a new category
    When the user creates a category with name "Streaming" and description "Video streaming services"
    Then the response status should be 201
    And the response body should contain the category name "Streaming"

  Scenario: Duplicate category name is rejected
    Given the category "Music" already exists
    When the user creates a category with name "Music" and description "Music services"
    Then the response status should be 409

  Scenario: List all categories
    Given the category "Productivity" already exists
    And the category "Gaming" already exists
    When the user requests all categories
    Then the response status should be 200
    And the category list should contain "Productivity"
    And the category list should contain "Gaming"
