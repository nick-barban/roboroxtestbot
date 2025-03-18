# Post Templates

This directory contains template files for various types of posts that can be used by the bot.

## Purpose

These templates define the structure and format of posts that will be sent to Telegram groups and channels. The templates support variable substitution to customize the content for specific scenarios.

## Template Format

Templates follow a markdown-like format with placeholders for dynamic content indicated by double curly braces:

```
# {{post_title}}

{{post_content}}

Posted by: {{author_name}}
```

## Usage

Templates can be referenced by name in the application code and used with the TemplateService to generate formatted post content with specific variables substituted.

Example:
```java
String formattedPost = templateService.renderTemplate("posts/announcement.md", variables);
``` 