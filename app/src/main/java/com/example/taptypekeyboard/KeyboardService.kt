package com.example.taptypekeyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

import kotlinx.coroutines.*

data class CursorInfo(val mode: String, val startPosition: Int?, val endPosition: Int?, val selectedText: String)


class KeyboardService : InputMethodService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private val emojis = "👍😎💩"
    private val lock = "\uD83D\uDD12"


    private val abcLayout = arrayOf(
        arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "⌫"),
        arrayOf(lock, "a", "s", "d", "f", "g", "h", "j", "k", "l", " ↵"),
        arrayOf("  ⇧ ", "z", "x", "c", "v", "b", "n", "m", ",", ".", "?", "!" ),
        arrayOf("123%","(", "                  ",  ")",  emojis)
        // Add other special keys or rows as needed
    )
    private val capsLayout = arrayOf(
        arrayOf("@", "#", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", " ⌫ "),
        arrayOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "(", ")"),
        arrayOf(lock, "A", "S", "D", "F", "G", "H", "J", "K", "L", "\"", " ↵ "),
        arrayOf("  ⇧ ", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "?", "!" ),
        arrayOf("123%", "👍", "                     ", ":)",  emojis)
        // Add other special keys or rows as needed
    )
    // Define rows of keys
    private val numLayout = arrayOf(
        arrayOf("@", "#", "_", "!", "/", "*", "<", "⌫"),
        arrayOf("&", "$", "7", "8", "9", "%", ">", "'"),
        arrayOf("/", "(", "4", "5", "6", "+", ")", ":"),
        arrayOf("?", "[", "1", "2", "3", "-", "]", ";"),
        arrayOf("abc", " ", " 0 ", " . ", "=", ",", emojis + "  ")

        // Add other special keys or rows as needed
    )
    private val emojiLayout = arrayOf(
        // Row 1: Most Popular Emotions & Gestures
        arrayOf("😂", "😍", "😎", "💀", "😇", "😢", "🥳", "😡", "⌫"),

        // Row 2: More weird faces and expressions
        arrayOf("😈", "🤡", "🤠", "🤑", "🤓", "🤖", "👽", "👾", "👻"),

        // Row 2: Activities & Celebrations
        arrayOf(":)", "👀", "🙈", "💩", "💃",  "🎉", "👅", "🚴‍♀️", "!"),

        // Row 3: Animals, Nature & Weather
        arrayOf("<3", "🚗", "🐱", "😤", "🙏", "🌳", "🔥", "❄️", "?"),

        // Row 4: Food, Objects & Symbols
        arrayOf("123%", "🍕", "               ",  "👍", "❤️", "abc")
    )

    private val keyboards = mapOf(
        "abc" to abcLayout,
        lock to capsLayout,
        "123%" to numLayout,
        emojis to emojiLayout
    )

    private var currentKeyboardName = "abc"
    private var currentKeyboard = keyboards[currentKeyboardName]!!
    private var nextKeyboard: String? = null

    private val iconLetters = "acbdefghijklmnopqrstuvwxyz"
    private val iconMapping = mapOf<String, String>(
        "0" to "icon_0",
        "1" to "icon_1",
        "2" to "icon_2",
        "3" to "icon_3",
        "4" to "icon_4",
        "5" to "icon_5",
        "6" to "icon_6",
        "7" to "icon_7",
        "8" to "icon_8",
        "9" to "icon_9",
        "⌫" to "backspace",
        "↵" to "enter",
        "." to "period",
        "," to "comma",
        ":" to "colon",
        ";" to "semicolon",
        "&" to "ampersand",
        "@" to "at",
        "*" to "asterisk",
        "?" to "question",
        "!" to "exclamation",
        "\"" to "quote",
        "#" to "hash",
        "123%" to "icon_123",
        "abc" to "abc",
        "" to "space",
        "(" to "open_parenth",
        ")" to "close_parenth",
        "[" to "open_bracket",
        "]" to "close_bracket",
        "<" to "lt",
        ">" to "gt",
        "-" to "dash",
        "'" to "tick",
        "/" to "fslash",
        "+" to "plus",
        "$" to "dollar",
        "=" to "equal",
        "⇧" to "up",
        "_" to "underscore",
        "%" to "percent",
    )


    override fun onCreateInputView(): View {
        // Initialize the keyboard layout
        return setKeyboardLayout(currentKeyboard)
    }

    public fun switchKeyboard(keyboardName: String, temporary: Boolean = false) {
        if (temporary) {
            nextKeyboard = currentKeyboardName
        }else{
            nextKeyboard = null
        }
        currentKeyboardName = keyboardName
        currentKeyboard = keyboards[currentKeyboardName]!!
        setInputView(onCreateInputView())

    }

    private fun setKeyboardLayout(keyboard: Array<Array<String>>): LinearLayout {
        val baseLayout = layoutInflater.inflate(R.layout.keyboard_layout, null) as LinearLayout

        val rowLayouts = makeKeyboardLayout(keyboard)
        // set the content of the keyboard_rows layout to the rowLayouts
        val keyboardRows = baseLayout.findViewById<LinearLayout>(R.id.keyboard_rows)
        keyboardRows.removeAllViews()
        rowLayouts.forEach { rowLayout ->
            keyboardRows.addView(rowLayout)
        }
        return baseLayout
    }

    private fun makeKeyboardLayout(keyboard: Array<Array<String>>): Array<LinearLayout> {
        val rowLayouts = arrayOfNulls<LinearLayout>(keyboard.size)
        var ind = 0
        keyboard.forEach { row ->
            val rowLayout = LinearLayout(this)
            rowLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            rowLayout.orientation = LinearLayout.HORIZONTAL

            // Calculate the total weight for the row based on the length of the labels
            val totalWeight = row.map { it.length }.sum().toFloat()
            // if keyLabel in a-z

            val resources = this.resources
            val packageName = this.packageName

            row.forEach { keyLabel ->
                val key = Button(this)
                val trimmedKeyLabel = keyLabel.trim()

                if (iconMapping.containsKey(trimmedKeyLabel)){
                    // Load the drawable resource
                    val resId = resources.getIdentifier(iconMapping.get(trimmedKeyLabel), "drawable", packageName)
                    val iconDrawable = ContextCompat.getDrawable(this, resId)

                    // Set the drawable as the button icon or background
                    key.background = iconDrawable
                }else if (trimmedKeyLabel != "" && iconLetters.contains(trimmedKeyLabel)){
                    // Load the drawable resource
                    val resId = resources.getIdentifier(trimmedKeyLabel, "drawable", packageName)
                    val iconDrawable = ContextCompat.getDrawable(this, resId)

                    // Set the drawable as the button icon or background
                    key.background = iconDrawable
                }else {
                    key.text = keyLabel
                }
                // Set the text size of the button
                key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f) // 18sp font size, change as needed

                key.setOnClickListener {
                    val trimmedLabel = keyLabel.trim()
                    when (trimmedLabel) {
                        "⇧" -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock, currentKeyboardName == "abc")
                        "123%" -> switchKeyboard("123%")
                        "abc" -> switchKeyboard("abc")
                        lock -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock)
                        emojis -> switchKeyboard(emojis)
                        "⌫" -> handleBackspace()
                        "↵" -> handleEnter()
                        "" -> inputText(" ")
                        else -> inputText(keyLabel)
                    }
                }

                // Set OnLongClick Listener
                key.setOnLongClickListener {
                    val trimmedLabel = keyLabel.trim()
                    when (trimmedLabel) {
                        "⇧" -> Unit
                        "123%" -> Unit
                        "abc" -> Unit
                        lock -> Unit
                        emojis -> Unit
                        "⌫" -> clearText()
                        "↵" -> Unit
                        else -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock, true)
                    }
                    true
                }
                key.setPadding(1, 1, 1, 1)

                // Calculate weight for each key based on its label length
                val keyWeight =  (2 + keyLabel.length.toFloat()) / totalWeight
                val keyLayoutParams = LinearLayout.LayoutParams(0, 130, keyWeight)
                keyLayoutParams.setMargins(1, 1, 1,1)
                key.layoutParams = keyLayoutParams

                rowLayout.addView(key)
            }

            rowLayouts[ind] = rowLayout
            ind += 1
        }
        return rowLayouts!! as Array<LinearLayout>
    }

    private fun inputText(text: String) {
        val inputConnection = currentInputConnection
        inputConnection?.commitText(text, 1)
        afterTextChange()
        if (nextKeyboard != null) {
            switchKeyboard(nextKeyboard!!)
        }
    }

    private fun handleEnter() {
        val editorInfo = currentInputEditorInfo
        nextKeyboard = null

        if (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0) {
            // Multi-line text field, insert newline
            inputText("\n")
        } else {
            // Single-line text field, submit text
            submit()
        }
    }

    private fun handleBackspace() {
        nextKeyboard = null
        val cursorInfo = getCursorInfo(true)

        val inputConnection = currentInputConnection
        val noSelection = cursorInfo.startPosition == cursorInfo.endPosition
        if (noSelection) inputConnection?.deleteSurroundingText(1, 0) else inputConnection?.commitText("", 0)

        afterTextChange()
    }

    public fun clearText() {
        // use inputConnection to delete all text in the text field
        nextKeyboard = null
        val inputConnection = currentInputConnection
        inputConnection?.deleteSurroundingText(getText().length, 0)

        afterTextChange()
    }

    public fun afterTextChange() {
    }
    public fun getCursorInfo(backspace: Boolean = false): CursorInfo {
        val inputConnection = currentInputConnection
        val request = ExtractedTextRequest()
        val extractedText = inputConnection?.getExtractedText(request, 0)
        val startPosition = extractedText?.selectionStart
        val endPosition = extractedText?.selectionEnd
        val length = extractedText?.text.toString().length

        val noSelection = startPosition == endPosition
        val startCursorAtEnd = startPosition == length
        val selectedText = getSelectedText()
        val mode = if (backspace){
            if (noSelection && startCursorAtEnd) "pop" else if (noSelection) "backspace" else "remove"
        }else{
            if (noSelection && startCursorAtEnd) "append" else if (noSelection) "insert" else "replace"
        }

        // return mode, start position, end position, and selected text as a tuple
        return CursorInfo(mode, startPosition, endPosition, selectedText)
    }

    public fun setCursor(startPosition: Int = -1, endPosition: Int? = null) {
        // -1 means end of text, null means no selection
        val inputConnection = currentInputConnection
        var start = startPosition
        if (startPosition == -1){
            start = getText().length
        }
        var end = endPosition
        if (endPosition == -1){
            end = getText().length
        }
        if (endPosition == null){
            end = start
        }
        inputConnection?.setSelection(start, end!!)
    }

    public fun getSelectedText(): String {
        val inputConnection = currentInputConnection
        val selectedText = inputConnection?.getSelectedText(0)
        return selectedText.toString()
    }

    public fun getText(): String {
        val inputConnection = currentInputConnection
        val request = ExtractedTextRequest()
        val extractedText = inputConnection?.getExtractedText(request, 0)
        return extractedText?.text.toString()
    }

    public fun setText(text: String, cursorPosition: Int = -1, endPosition: Int? = null) {
        val inputConnection = currentInputConnection
        inputConnection?.beginBatchEdit()

        // Move the cursor to the start of the text
        inputConnection?.setSelection(0, 0)

        // Get the total length of the current text
        val extractedText = inputConnection?.getExtractedText(ExtractedTextRequest(), 0)
        val length = extractedText?.text?.length ?: 0

        // Delete the entire text
        inputConnection?.deleteSurroundingText(0, length)

        // Insert the new text
        inputConnection?.commitText(text, 1)

        if (cursorPosition != -1){
            setCursor(cursorPosition, endPosition)
        }

        inputConnection?.endBatchEdit()
    }

    public fun submit() {
        // Implement logic to submit the text
        currentInputConnection?.performEditorAction(1)
    }

}
