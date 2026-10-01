package dev.efm.solaris_core.functions.backToDeath;

import java.util.ArrayList;
import java.util.List;

public class PlayerDListCap implements IPlayerDListCap {
    private final List<PlayerPositionData> dataList = new ArrayList<>();

    @Override
    public List<PlayerPositionData> getDList() {
        return dataList;
    }

    @Override
    public void setDList(List<PlayerPositionData> list) {
        this.dataList.clear();
        this.dataList.addAll(list);
    }

    @Override
    public void addData(PlayerPositionData data) {
        this.dataList.add(0, data);
        if (dataList.size() > 10) {
            dataList.remove(10);
        }
    }

    @Override
    public void clearData() {
        this.dataList.clear();
    }
}
