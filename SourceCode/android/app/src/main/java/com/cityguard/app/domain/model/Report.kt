package com.cityguard.app.domain.model


enum class VoteType {
    CONFIRMED,   // "Tôi cũng gặp tình trạng này"
    NO_LONGER    // "Không còn nữa"
}

data class Comment(
    val id: String,
    val authorName: String,
    val content: String,
    val createdAtLabel: String   // tạm là chuỗi như "1 phút trước"; API thật sẽ trả mốc thời gian
)

data class Report(
    val id: String,
    val category: IncidentCategory,    // dùng lại enum của TV3
    val description: String,
    val address: String,
    val createdAtLabel: String,
    val trustLabel: String,            // "Cao", "Thấp"...; do backend tính
    val confirmCount: Int,
    val noLongerCount: Int,
    val myVote: VoteType?,             // null nghĩa là chưa vote
    val imageUri: String? = null,
    val comments: List<Comment> = emptyList()
)