import os
import re
from collections import defaultdict

root_dir = "c:/Users/L900m/OneDrive/Desktop/OpeNLogin-master/OpeNLogin-master"

findings = defaultdict(list)

def add_finding(category, file_path, line_no, content, reason):
    file_name = os.path.basename(file_path)
    findings[category].append(f"{file_name}:{line_no} | {reason} -> {content.strip()}")

for dirpath, dirnames, filenames in os.walk(root_dir):
    for filename in filenames:
        if filename.endswith(".java"):
            filepath = os.path.join(dirpath, filename)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    lines = f.readlines()
            except:
                continue

            for i, line in enumerate(lines):
                line_no = i + 1
                
                # Check for blocking calls
                if re.search(r'\.join\(\)', line) or re.search(r'\.get\(\)', line) and 'Future' in ''.join(lines[max(0, i-5):i+5]):
                    add_finding("Concurrency - Blocking Future", filepath, line_no, line, "Possible blocking call (.join()/.get())")
                
                # Check for thread creation
                if re.search(r'new Thread\(', line):
                    add_finding("Concurrency - Raw Thread", filepath, line_no, line, "Raw Thread created instead of using Scheduler/Executor")
                
                # Check for DB operations
                if re.search(r'(?i)\b(SELECT|UPDATE|INSERT|DELETE)\b', line) and 'executeQuery' in ''.join(lines[max(0, i-5):i+5]):
                    add_finding("Database - Potential Sync DB Call", filepath, line_no, line, "SQL Query found, check if sync")
                
                # Check for static collections
                if re.search(r'static\s+.*(Map|List|Set|HashMap|ArrayList|HashSet|ConcurrentHashMap)\s*<', line):
                    add_finding("Memory - Static Collection", filepath, line_no, line, "Static collection could cause memory leak")
                
                # Check for Player references in collections
                if re.search(r'(Map|List|Set|HashMap|ArrayList|HashSet|ConcurrentHashMap)\s*<.*Player', line):
                    add_finding("Memory - Player Reference", filepath, line_no, line, "Storing Player objects in collections can cause memory leaks")

                # Check for synchronized keyword
                if re.search(r'\bsynchronized\b', line):
                    add_finding("Concurrency - Synchronized", filepath, line_no, line, "Usage of synchronized block/method")

                # Check for Scheduler runNow
                if re.search(r'runNow\(', line):
                    add_finding("Paper - runNow", filepath, line_no, line, "Folia/Paper async scheduler runNow() might break Spigot")

                # Check for runTaskTimer or schedule
                if re.search(r'runTaskTimer|scheduleAtFixedRate', line):
                    add_finding("Memory/Perf - Repeating Task", filepath, line_no, line, "Repeating task, check if it's ever cancelled")

for category, items in findings.items():
    print(f"=== {category} ===")
    for item in items:
        print(item)
    print()
