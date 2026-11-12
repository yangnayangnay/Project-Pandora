package com.example.project_pandora.ui.mine;

import android.text.InputFilter;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.widget.EditText;
import com.example.project_pandora.core.util.SignatureRules;

/** Limits rendered lines as well as line breaks; maxLines alone would only hide overflow. */
public final class SignatureLineFilter implements InputFilter {
    private final EditText editor;
    private final Runnable rejected;
    public SignatureLineFilter(EditText editor, Runnable rejected) { this.editor = editor; this.rejected = rejected; }

    @Override public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (start == end) return null; // Allow repairs to an older signature without truncating stored data.
        CharSequence incoming = source.subSequence(start, end);
        String normalized = SignatureRules.normalize(incoming);
        String candidate = dest.subSequence(0, dstart).toString() + normalized + dest.subSequence(dend, dest.length());
        if (!fits(editor, candidate)) {
            if (rejected != null) rejected.run();
            return dest.subSequence(dstart, dend); // A rejected replacement must preserve the selected text.
        }
        return normalized.contentEquals(incoming) ? null : normalized;
    }

    public static boolean fits(EditText editor, CharSequence value) {
        String text = SignatureRules.normalize(value);
        if (SignatureRules.explicitLines(text) > SignatureRules.MAX_LINES) return false;
        int width = editor.getWidth() - editor.getCompoundPaddingLeft() - editor.getCompoundPaddingRight();
        if (width <= 0) return true; // Rechecked at layout and before saving.
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), editor.getPaint(), width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(editor.getIncludeFontPadding())
                .setLineSpacing(editor.getLineSpacingExtra(), editor.getLineSpacingMultiplier())
                .setBreakStrategy(editor.getBreakStrategy()).setHyphenationFrequency(editor.getHyphenationFrequency()).build();
        return layout.getLineCount() <= SignatureRules.MAX_LINES;
    }
}
