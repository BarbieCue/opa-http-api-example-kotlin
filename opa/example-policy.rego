package httpapi.auth_example

# bob is alice's manager, and betty is charlie's.
subordinates := {"alice": [], "charlie": [], "bob": ["alice"], "betty": ["charlie"]}

# Allow users to get their own salaries.
allow {
	input.method == "GET"
	input.path == ["finance", "salary", input.user]
}

# Logical OR (https://www.openpolicyagent.org/docs/latest/#logical-or)

# Allow managers to get their subordinates' salaries.
allow {
	some username
	input.method == "GET"
	input.path = ["finance", "salary", username]
	subordinates[input.user][_] == username
}