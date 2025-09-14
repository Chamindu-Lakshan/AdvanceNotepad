# AdvancedNotepad

A modern, feature-rich text editor built in Java using Swing, designed as a single-file application with an enhanced user interface for a seamless note-taking and editing experience.

## Features

- **Multi-Tab Support**: Open and edit multiple files in separate tabs with custom, gradient-styled tab headers and animated close buttons.
- **Dark/Light Theme**: Toggle between dark and light themes for comfortable viewing in different lighting conditions.
- **File Operations**: Create, open, save, and save-as text files (.txt) with a file chooser dialog.
- **Recent Files**: Tracks recently opened files (up to 8) for quick access via the "Recent Files" menu.
- **Autosave & Recovery**: Automatically saves modified tabs every 60 seconds and offers recovery of unsaved changes on startup.
- **Find & Replace**: Search and replace text with case-sensitive options in the current tab.
- **Print/Export**: Print documents or export to PDF using the system’s print dialog.
- **Font Customization**: Choose fonts, styles, and sizes with a preview dialog; customize text and background colors.
- **Undo/Redo**: Full support for undo and redo operations per tab.
- **Modern UI**: Features gradient buttons, animated caret, hover effects, and a sleek tabbed interface with shadow effects for active tabs.

## Requirements

- **Java Runtime Environment (JRE)**: Java 8 or higher.
- **Operating System**: Windows, macOS, or Linux (any OS supporting Java).

## Installation

1. **Download the Source**:
   - Clone or download the `AdvancedNotepad.java` file from this repository.

2. **Compile the Application**:
   ```bash
   javac AdvancedNotepad.java
   ```

3. **Run the Application**:
   ```bash
   java AdvancedNotepad
   ```

## Usage

1. **Launch the Application**:
   - Run the compiled Java program to open the AdvancedNotepad window.

2. **Key Features**:
   - **New Tab**: Click "New" (Ctrl+N) to create a new tab.
   - **Open File**: Use "Open" (Ctrl+O) to load a text file into a new tab.
   - **Save/Save As**: Save changes with "Save" (Ctrl+S) or "Save As" to choose a new file location.
   - **Find & Replace**: Access via the "Find/Replace" button to search and modify text.
   - **Toggle Theme**: Switch between dark and light modes using the "Toggle Theme" button.
   - **Font Customization**: Go to "Format > Font..." (Ctrl+T) to change font settings.
   - **Recent Files**: Access recently opened files from the "File > Recent Files" menu.
   - **Print/Export**: Use "Print/Export" to print or save as PDF.

3. **Closing Tabs**:
   - Click the "×" button on a tab or use the right-click menu to close. You’ll be prompted to save changes if the tab is modified.

4. **Autosave**:
   - Modified tabs are autosaved every 60 seconds to `~/.advancednotepad_autosave/`. On startup, you’ll be prompted to recover unsaved changes.

## Screenshots

*Note: Add screenshots of the application (e.g., dark mode, light mode, tab interface) to the repository for better visualization.*

## Notes

- The application uses a single Java file (`AdvancedNotepad.java`) for simplicity, but it can be refactored into multiple files for larger projects (e.g., separate classes for `GradientButton`, `TabHeader`, etc.).
- The application icon (`icon.png`) is optional. If not found, a fallback gradient icon is generated.
- Autosaved files are stored in `~/.advancednotepad_autosave/` and recent files in `~/.advancednotepad_recent`.

## Limitations

- The application is designed for text files (.txt) and does not support advanced formatting or other file types.
- Printing to PDF depends on the system’s print dialog supporting a PDF output option.
- No support for drag-and-drop file opening or advanced text editing features like syntax highlighting.

## Future Improvements

- Add syntax highlighting for code files.
- Support additional file formats (e.g., Markdown, RTF).
- Implement drag-and-drop functionality for opening files.
- Add word count and line count indicators.
- Enhance accessibility with keyboard navigation for all UI elements.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Author
W.Chamindu Lakshan 
////university of colombo
