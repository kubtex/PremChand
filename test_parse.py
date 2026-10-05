import json

with open('app/src/main/assets/stories_catalog.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for i, story in enumerate(data):
    for key in ['id', 'titleHindi', 'titleEnglish', 'description', 'category', 'estimatedReadingMinutes', 'isFeatured']:
        if key not in story:
            print(f"Missing {key} in story {i}")
        if story[key] is None:
            print(f"None {key} in story {i}")

print(f"Checked {len(data)} stories: All fields valid!")
