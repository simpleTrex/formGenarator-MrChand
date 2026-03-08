package com.adaptivebp.modules.uibuilder.dto;

import java.util.List;
import com.adaptivebp.modules.uibuilder.model.LayoutItem;

public class SaveLayoutRequest {
    private List<LayoutItem> layout;

    public List<LayoutItem> getLayout() {
        return layout;
    }

    public void setLayout(List<LayoutItem> layout) {
        this.layout = layout;
    }
}
