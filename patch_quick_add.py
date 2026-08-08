import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_quick_items = """                        val quickItems = listOf(
                            "Groceries" to "Food",
                            "Transport" to "Transport",
                            "Rent" to "Housing",
                            "Coffee" to "Food",
                            "Movies" to "Entertainment"
                        )"""

new_quick_items = """                        val quickItems = listOf(
                            LocalAppStrings.current.qGroceries to LocalAppStrings.current.catFood,
                            LocalAppStrings.current.qTransport to LocalAppStrings.current.catTransport,
                            LocalAppStrings.current.qRent to LocalAppStrings.current.catHousing,
                            LocalAppStrings.current.qCoffee to LocalAppStrings.current.catFood,
                            LocalAppStrings.current.qMovies to LocalAppStrings.current.catEntertainment
                        )"""
content = content.replace(old_quick_items, new_quick_items)

old_cat = """    val categories = listOf("Food", "Transport", "Entertainment", "Utilities", "Housing", "Other")
    var category by remember { mutableStateOf(initialCategory ?: categories[0]) }"""

new_cat = """    val categories = listOf(LocalAppStrings.current.catFood, LocalAppStrings.current.catTransport, LocalAppStrings.current.catEntertainment, LocalAppStrings.current.catUtilities, LocalAppStrings.current.catHousing, LocalAppStrings.current.catOther)
    var category by remember { mutableStateOf(initialCategory ?: categories[0]) }"""
content = content.replace(old_cat, new_cat)

old_category_dropdown = """                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(LocalAppStrings.current.category) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )"""

new_category_dropdown = """                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        readOnly = false,
                        label = { Text(LocalAppStrings.current.category) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )"""
content = content.replace(old_category_dropdown, new_category_dropdown)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
