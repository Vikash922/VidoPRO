import re

path = 'feature/timeline/src/main/java/com/example/feature/timeline/ui/ClipCard.kt'
with open(path, 'r') as f:
    content = f.read()

# Add imports
imports = """
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.absoluteValue
"""
content = re.sub(r'(import androidx.compose.ui.Modifier)', r'\1' + imports, content)

# Fix sumOf { it.positionChange().x } -> sumOf { it.positionChange().x.toDouble() }.toFloat()
content = re.sub(
    r'sumOf \{ \(it.position.x - touchStartX\).absoluteValue \}',
    r'map { (it.position.x - touchStartX).absoluteValue }.sum()',
    content
)

content = re.sub(
    r'sumOf \{ it.positionChange\(\).x \}',
    r'map { it.positionChange().x }.sum()',
    content
)

# Fix consumeAllChanges() -> consume()
content = content.replace('change.consumeAllChanges()', 'changes.forEach { it.consume() }')
content = content.replace('change.consume()', 'changes.forEach { it.consume() }')

with open(path, 'w') as f:
    f.write(content)
