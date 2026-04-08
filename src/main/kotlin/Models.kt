package com.infinitybutterfly

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable

// 1. Updated Users Table
object Users : IntIdTable() {
    val email = varchar("email", 100).uniqueIndex()
    val otpCode = varchar("otp_code", 6).nullable()
    val isVerified = bool("is_verified").default(false)

    // Profile Data (For your Android Cache)
    val name = varchar("name", 100).nullable()
    val username = varchar("username", 50).nullable()
    val country = varchar("country", 50).nullable()
    val dob = varchar("dob", 20).nullable()
    val bio = text("bio").nullable()
    val isProfileComplete = bool("is_profile_complete").default(false)
    val profileImageUrl = varchar("profile_image_url", 255).nullable()

//    Fav Food and Allergies Selected by the User
    val favFoods = text("fav_foods").nullable()
    val allergies = text("allergies").nullable()
}

// 2. New Recipes Table
object Recipes : IntIdTable() {
    val name = varchar("name", 255)
    val country = varchar("country", 100)
    val category = varchar("category", 100)
    val tags = varchar("tags", 255)
    val instructions = text("instructions")
    val ingredientsName = text("ingredients_name")
    val ingredientsQuantity = text("ingredients_quantity")
    val imageUrl = varchar("image_url", 255)
    val userName = varchar("user_name", 255).nullable


    val userId = reference("user_id", Users) // Links recipe to the creator
}

// 3. Help Center

object HelpTickets : IntIdTable() {
    val title = varchar("title", 255)
    val description = text("description")
    val userId = reference("user_id", Users) // Links the ticket to the user who sent it
}


// --- API DATA MODELS ---

@Serializable
data class ProfileListResponse(
    val success: Boolean,
    val results: List<UserProfile>
)

@Serializable
data class EmailOtpRequest(val email: String)

@Serializable
data class VerifyEmailRequest(val email: String, val otp: String)

// This perfectly matches the data your Android app wants to cache!
@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String,
    val isProfileComplete: Boolean,
    val profileData: UserProfile?
)

@Serializable
data class RecipeSummary(
    val id: String,
    val imageUrl: String?,
    val recipeName: String,
    val countryName: String,
    val category: String
)

@Serializable
data class UserProfile(
    val email: String?,
    val name: String?,
    val username: String?,
    val country: String?,
    val dob: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val isProfileComplete: Boolean,
    val favFoods: String?,
    val allergies: String?,

    val recipes: List<RecipeSummary> = emptyList()

)

@Serializable
data class RecipeResponse(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val category: String,
    val country: String,
    val tags: String,
    val instructions: String,
    val ingredientsName: String,
    val ingredientsQuantity: String,
    val userName: String?
)

//@Serializable
//data class SubmitProblemResponse(
//    val title: String,
//    val description: String
//)

@Serializable
data class SimpleMessageResponse(val success: Boolean, val message: String)

@Serializable
data class RecipeSearchResponse(val success: Boolean, val results: List<RecipeResponse>)

@Serializable
data class HelpRequest(val title: String, val description: String)

@Serializable
data class RecipeFeedResponse(
    val success: Boolean,
    val recipes: List<RecipeResponse>
)




//package com.infinitybutterfly
//
//import kotlinx.serialization.Serializable
//import org.jetbrains.exposed.dao.id.IntIdTable
//
//// --- DATABASE TABLES ---
//
//object Users : IntIdTable() {
//    val phoneNumber = varchar("phone_number", 20).uniqueIndex()
//    val otpCode = varchar("otp_code", 6).nullable() // Temporarily holds the 6-digit code
//    val role = varchar("role", 20).default("user")
//}
//
//object Collections : IntIdTable() {
//    val name = varchar("name", 100)
//    val userId = reference("user_id", Users)
//}
//
//object Texts : IntIdTable() {
//    val content = text("content")
//    val collectionId = reference("collection_id", Collections).nullable()
//    val userId = reference("user_id", Users)
//}
//
//object Images : IntIdTable() {
//    val url = varchar("url", 255)
//    val collectionId = reference("collection_id", Collections).nullable()
//    val userId = reference("user_id", Users)
//}
//
//// --- API REQUEST/RESPONSE MODELS ---
//
//@Serializable
//data class OtpRequest(val phoneNumber: String)
//
//@Serializable
//data class VerifyRequest(val phoneNumber: String, val otp: String)
//
//@Serializable
//data class SimpleMessageResponse(val success: Boolean, val message: String)
//
//@Serializable
//data class TokenResponse(val success: Boolean, val token: String)
//
//@Serializable
//data class MyImagesResponse(val success: Boolean, val images: List<ImageResponse>)
//
//@Serializable
//data class ImageResponse(val id: Int, val url: String)
//
//
//
//
//
//// USING EMAIL AND PASSWORD LOGIN AND REGISTER
//
//
////import org.jetbrains.exposed.dao.id.IntIdTable
////import kotlinx.serialization.Serializable
////
////// ADD THIS DATA CLASS:
////@Serializable
////data class RegisterRequest(
////    val username: String,
////    val email: String,
////    val password: String
////)
////@Serializable
////data class LoginRequest(
////    val email: String,
////    val password: String
////)
////@Serializable
////data class MyImagesResponse(
////    val success: Boolean,
////    val images: List<ImageResponse>
////)
////@Serializable
////data class ImageResponse(
////    val id: Int,
////    val url: String
////)
////// Represents the "users" table in PostgreSQL
////object Users : IntIdTable() {
////    val username = varchar("username", 50)
////    val email = varchar("email", 100).uniqueIndex()
////    val password = varchar("password", 255)
////    val role = varchar("role", 20).default("user")
////}
////
////// Represents the "images" table
////object Images : IntIdTable() {
////    val url = varchar("url", 255)
////    // This creates a Foreign Key linking the image to a specific user
////    val userId = reference("user_id", Users)
////}
