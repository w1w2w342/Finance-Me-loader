with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()
for i, line in enumerate(lines):
    if 'fun ExpenseBarChart(' in line:
        start = max(0, i - 2)
        end = min(len(lines), i + 40)
        print("".join(lines[start:end]))
        break
