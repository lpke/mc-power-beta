# Owner review report

Use the `$html` skill after a requested release. Read its destination repository's
instructions and save a separate project document for that version, such as
`main--4.4.0-review.html`. Keep earlier reports as historical records.

Make a compact checklist the owner can read from top to bottom while testing:

- Group changes by the user's categories, in their requested order.
- Cover every requested point, including small visual and default changes.
- Give each point a short result. "Implemented" is enough when no decision needs explanation.
- State material decisions, migrations, differences and anything unfinished plainly.
- Include the exact names of removed settings and what replaces them.
- Add optional interactive checkboxes that remember this report's review progress locally.
- Keep technical notes brief. Explain practical behavior before implementation details.
- End with concise validation, release and instance-update status. Do not claim unrun tests.

Do not repeat the prompt or turn the file into a session diary. Avoid long tables,
performance claims without measurements, filler and explanatory text that adds no
review value. Keep private paths, account details, logs and reports out of the public
modpack repository. The HTML library owns its own commits and metadata rules.
