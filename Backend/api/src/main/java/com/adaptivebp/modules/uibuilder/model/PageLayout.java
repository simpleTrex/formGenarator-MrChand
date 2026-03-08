package com.adaptivebp.modules.uibuilder.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "page_layouts")
@CompoundIndexes({
        @CompoundIndex(name = "page_layout_idx", def = "{'pageId':1,'appId':1}", unique = true)
})
public class PageLayout {
    @Id
    private String id;
    private String pageId;
    private String appId;
    private List<LayoutItem> layout = new ArrayList<>();
    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public List<LayoutItem> getLayout() {
        return layout;
    }

    public void setLayout(List<LayoutItem> layout) {
        this.layout = layout;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
