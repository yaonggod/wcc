package com.ssafy.wcc.domain.topic.application.mapper;

import com.ssafy.wcc.domain.topic.application.dto.response.TopicResponse;
import com.ssafy.wcc.domain.topic.application.dto.response.TopicTypeDetailResponse;
import com.ssafy.wcc.domain.topic.db.entity.Topic;
import com.ssafy.wcc.domain.topic.db.entity.TopicTypeDetail;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TopicTypeDetailMapper {
    TopicTypeDetailMapper INSTANCE = Mappers.getMapper(TopicTypeDetailMapper.class);

    public TopicTypeDetailResponse toTopicTypeDetailResponse(TopicTypeDetail topicTypeDetail);
}
