import os
import re

lang_dir = '.'
files = [f for f in os.listdir(lang_dir) if f.startswith('messages_') and f.endswith('.yml')]

title_add = """      bedrock-auto-login:
        title: "&a&lWelcome!"
        subtitle: "&7Logged in as &eBedrock&7."
        delays:
          start: 0
          duration: 30
          end: 6
      premium-auto-login:
        title: "&a&lWelcome!"
        subtitle: "&7Logged in as &6Premium&7."
        delays:
          start: 0
          duration: 30
          end: 6"""

error_add = """    insecure-password: "&cYour password is too weak. It must include numbers, uppercase, lowercase, and symbols."
    change-password-enforced: "&cFor security reasons, you must update your password right now using &e/changepassword&c."
"""

for f in files:
    if f == 'messages_es.yml':
        continue
    filepath = os.path.join(lang_dir, f)
    with open(filepath, 'r', encoding='utf-8') as file:
        content = file.read()
    
    # Check and add to Title:
    if 'bedrock-auto-login:' not in content and 'Title:' in content:
        # Find 'after-register:' block end
        after_reg_pos = content.find('after-register:')
        if after_reg_pos != -1:
            # find next delay-kick
            delay_kick_pos = content.find('delay-kick:', after_reg_pos)
            if delay_kick_pos != -1:
                # Insert before delay-kick
                content = content[:delay_kick_pos] + title_add + '\n\n  ' + content[delay_kick_pos:]
    
    # Check and add to error-messages:
    if 'insecure-password:' not in content and 'error-messages:' in content:
        password_too_small_pos = content.find('password-too-small:')
        if password_too_small_pos != -1:
            # Find the end of this line
            newline_pos = content.find('\n', password_too_small_pos)
            if newline_pos != -1:
                content = content[:newline_pos+1] + error_add + content[newline_pos+1:]
    
    with open(filepath, 'w', encoding='utf-8') as file:
        file.write(content)

print("Updated all language files.")
