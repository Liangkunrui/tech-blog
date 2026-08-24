package com.blog.vo;

import com.blog.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论视图对象
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    private Long id;
    private Long articleId;
    private Long parentId;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;

    public static CommentVO from(Comment comment) {
        return new CommentVO(comment.getId(), comment.getArticleId(), comment.getParentId(),
                comment.getContent(), comment.getStatus(), comment.getCreateTime(),
                comment.getUserId(), null, null, null);
    }
}
