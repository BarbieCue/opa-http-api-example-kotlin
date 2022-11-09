package httpapi.auth_example

test_get_own_salary_allowed {
    allow with input as {"path": ["finance", "salary", "bob"], "method": "GET", "user": "bob"}
}

test_manager_get_subordinate_salary_allowed {
    allow with input as {"path": ["finance", "salary", "alice"], "method": "GET", "user": "bob"}
}

test_get_salary_anonymous_denied {
    not allow with input as {"path": ["finance", "salary", "bob"], "method": "GET", "user": ""}
}

test_get_another_salary_denied {
    not allow with input as {"path": ["finance", "salary", "bob"], "method": "GET", "user": "charlie"}
}