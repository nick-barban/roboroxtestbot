# 📅 Daily Post - {{date}}

## Today's Agenda

{{#if has_events}}
### Events:
{{#each events}}
- **{{time}}**: {{name}} {{#if location}}at {{location}}{{/if}}
{{/each}}
{{else}}
No scheduled events for today.
{{/if}}

## Reminders
{{#each reminders}}
- {{this}}
{{/each}}

## Quote of the Day
> {{quote}}
> — {{quote_author}}

Have a great day! 🌟
#daily 