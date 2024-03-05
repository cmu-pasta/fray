import json

data = json.load(open("/home/aoli/repos/sfuzz/benchmarks/build/report/timeline.json"))

hist_data = {}

for timeline in data:
    for item in timeline:
        t = item["type"]
        if t not in hist_data:
            hist_data[t] = 0
        hist_data[t] += 1

print(hist_data)