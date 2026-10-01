package dev.efm.solaris_core.functions.resProc;

public class PlayerDurData implements IPlayerDurData{
    private int value = 0;
    @Override
    public int getValue() {
        return this.value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }
}
