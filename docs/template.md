# Documentation Template

This is a template for writing documentation in the `docs` folder. Follow this structure and format for consistency.

## File Structure

Use the following sections as a guide:

1. **Title** - One concise H1 heading
2. **Overview** - Brief introduction
3. **Main Sections** - H2 headings for major topics
4. **Subsections** - H3 headings for detailed topics
5. **Examples** - Code blocks with syntax highlighting
6. **Next Steps** - Links to related documentation

## Writing Guidelines

### Headers
```markdown
# Main Title (H1 - Use only once per file)
## Section Title (H2 - Major topics)
### Subsection Title (H3 - Detailed topics)
```

### Code Blocks

Always specify the language for syntax highlighting:

```bash
# Shell commands
docker run -d -p 8080:8080 logwise/logwise
```

```yaml
# Configuration files
version: '3.8'
services:
  logwise:
    image: logwise/logwise:latest
```

```json
{
  "example": "data",
  "format": "json"
}
```

### Lists

Use bullet points for unordered lists:
- Item one
- Item two
- Item three

Use numbered lists for steps:
1. First step
2. Second step
3. Third step

### Links

Link to other documentation:
- [Architecture docs](architecture.md)
- [API Reference](api.md)

### Important Notes

> Use blockquotes for important notes or warnings

## Example Document Structure

```markdown
# Title of Your Documentation

Brief overview of what this document covers.

## Prerequisites

What users need before starting:
- Requirement 1
- Requirement 2

## Main Topic 1

Detailed explanation...

### Sub-topic

More details...

### Code Example

\`\`\`bash
# Example command
command --options
\`\`\`

## Main Topic 2

More content...

## Related Documentation

- [Link to related doc](related.md)
- [Another link](another.md)
```

## Best Practices

1. **Be concise** - Get to the point quickly
2. **Use examples** - Show, don't just tell
3. **Keep updated** - Update docs when code changes
4. **Use consistent terminology** - Stick to project conventions
5. **Add links** - Connect related documentation
