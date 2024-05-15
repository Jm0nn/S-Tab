package com.sixb.note.entity;

import com.sixb.note.common.BaseTimeEntity;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@Data
@Node("Page")
@SuperBuilder
public class Page extends BaseTimeEntity {

    @Id
    @Property("pageId")
    private String pageId;

    @Property("noteId")
    private String noteId;

    @Property("template")
    private String template;

    @Property("color")
    private String color;

    @Property("direction")
    private Integer direction;

    @Property("pdfUrl")
    private String pdfUrl;

    @Property("pdfPage")
    private Integer pdfPage;

    @Property("pageData")
    private String pageData;

    @Relationship(type = "NextPage")
    private Page NextPage;

}
