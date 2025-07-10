package com.plsqlrewriter.parser.antlr.generated;

import org.antlr.v4.runtime.*;

public abstract class PlSqlParserBase extends Parser
{
    private boolean _isVersion12 = true;
    private boolean _isVersion10 = true;
    public PlSqlParserBase self;

    public PlSqlParserBase(TokenStream input) {
        super(input);
        self = this;
    }

    public boolean isVersion12() {
        return _isVersion12;
    }

    public void setVersion12(boolean value) {
        _isVersion12 = value;
    }

    public boolean isVersion10() {
        return _isVersion10;
    }

    public void setVersion10(boolean value) {
        _isVersion10 = value;
    }
    public boolean isTableAlias() {
        String lt1 = _input.LT(1).getText().toLowerCase();
        String lt2 = "";
        if (_input.LT(2).getText() != null){
            lt2 = _input.LT(2).getText().toLowerCase();
        }
        if ((lt1.equals("partition") && lt2.equals("by")) || lt1.equals("cross")
                || lt1.equals("natural") || lt1.equals("inner")
                || lt1.equals("join")
                || ((lt1.equals("full") || lt1.equals("left") || lt1.equals("right")) && (lt2.equals("outer") || lt2.equals("join")))) {
            return false;
        }
        return true;
    }
}
