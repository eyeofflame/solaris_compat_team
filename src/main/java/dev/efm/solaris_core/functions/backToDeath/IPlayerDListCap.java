package dev.efm.solaris_core.functions.backToDeath;

import java.util.List;

public interface IPlayerDListCap {
    List<PlayerPositionData> getDList();
    void setDList(List<PlayerPositionData> list);
    void addData(PlayerPositionData data);
    void clearData();
}
