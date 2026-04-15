package com.infinitybutterfly

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.infinitybutterfly.Users.username
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import jdk.internal.util.StaticProperty.userName
import org.jetbrains.exposed.sql.update
import java.util.Date
import org.jetbrains.exposed.sql.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.String
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.*
// import java.util.Properties
// import javax.mail.*
// import javax.mail.internet.*
import io.ktor.utils.io.toByteArray

// 1. Load the hidden variables from your .env file
val dotenv = dotenv { ignoreIfMissing = true }

// 2. Initialize the Cloudinary client using your secret URL
val cloudinary = Cloudinary(dotenv["CLOUDINARY_URL"])

fun Application.configureRouting() {
    routing {

        // ROUTE 1: Request Email OTP

        post("/api/request-otp") {
            val request = try {
                call.receive<EmailOtpRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Invalid email format"))
                return@post
            }

            val formattedEmail = request.email.lowercase().trim()

            transaction {
                // FIXED: Using selectAll().where to fix Exposed deprecation warning
                val existingUser = Users.selectAll().where { Users.email eq formattedEmail }.singleOrNull()

                if (existingUser != null) {
                    Users.update({ Users.email eq formattedEmail }) { it[otpCode] = "123456" }
                } else {
                    Users.insert {
                        it[email] = formattedEmail
                        it[otpCode] = "123456"
                    }
                }
            }

            call.respond(
                HttpStatusCode.OK,
                SimpleMessageResponse(success = true, message = "OTP Sent")
            )
        }
        
    //     post("/api/request-otp") {
    //         val request = call.receive<EmailOtpRequest>()
    // val otp = (100000..999999).random().toString()

    // try {
    //     sendOtpEmail(request.email, otp)
    //     // Only if the line above SUCCEEDS does this run:
    //     call.respond(HttpStatusCode.OK, SimpleMessageResponse(true, "OTP Sent"))
    // } catch (e: Exception) {
    //     // If email fails, the app gets a 500 error and STAYS on the login screen
    //     call.respond(HttpStatusCode.InternalServerError, SimpleMessageResponse(false, e.message ?: "Email failed"))
    // }
    //         // val request = try { call.receive<EmailOtpRequest>() } catch (_: Exception) {
    //         //     call.respond(HttpStatusCode.BadRequest, "Invalid data")
    //         //     return@post
    //         // }

    //         // val generatedOtp = (100000..999999).random().toString()
    //         // val formattedEmail = request.email.lowercase().trim()

    //         // transaction {
    //         //     // FIXED: Using selectAll().where to fix Exposed deprecation warning
    //         //     val existingUser = Users.selectAll().where { Users.email eq formattedEmail }.singleOrNull()

    //         //     if (existingUser != null) {
    //         //         Users.update({ Users.email eq formattedEmail }) { it[otpCode] = generatedOtp }
    //         //     } else {
    //         //         Users.insert {
    //         //             it[email] = formattedEmail
    //         //             it[otpCode] = generatedOtp
    //         //         }
    //         //     }
    //         // }

    //         // // 3. SEND THE REAL EMAIL!
    //         // sendOtpEmail(formattedEmail, generatedOtp)

    //         // call.respond(HttpStatusCode.OK, SimpleMessageResponse(success = true, message = "OTP Sent to Email"))
    //     }

        // ROUTE 2: Verify OTP and Download Profile Cache
        // post("/api/verify-otp") {
        //     val request = try { call.receive<VerifyEmailRequest>() } catch (_: Exception) {
        //         call.respond(HttpStatusCode.BadRequest, "Invalid data")
        //         return@post
        //     }

        //     val formattedEmail = request.email.lowercase().trim()

        //     val userRow = transaction {
        //         // FIXED: Using selectAll().where
        //         Users.selectAll().where { Users.email eq formattedEmail }.singleOrNull()
        //     }

        //     val savedOtp = userRow?.get(Users.otpCode)
        //     if (userRow == null || savedOtp == null || savedOtp != request.otp) {
        //         call.respond(HttpStatusCode.Unauthorized, SimpleMessageResponse(success = false, message = "Invalid OTP"))
        //         return@post
        //     }

        //     // Clear OTP
        //     transaction { Users.update({ Users.email eq formattedEmail }) { it[otpCode] = null } }

        //     // Generate JWT
        //     val jwtSecret = dotenv["JWT_SECRET"] ?: throw Exception("Missing Secret")
        //     val token = JWT.create()
        //         .withIssuer("http://localhost:8080/")
        //         // .withIssuer("https://infiflyrecipeserver.onrender.com/")
        //         .withClaim("user_id", userRow[Users.id].value.toString())
        //         .withExpiresAt(Date(System.currentTimeMillis() + 604800000))
        //         .sign(Algorithm.HMAC256(jwtSecret))

        //     // Package the Profile Data for the Android Cache!
        //     val profileData = UserProfile(
        //         name = userRow[Users.name],
        //         username = userRow[Users.username],
        //         country = userRow[Users.country],
        //         dob = userRow[Users.dob],
        //         bio = userRow[Users.bio],
        //         profileImageUrl = userRow[Users.profileImageUrl],
        //         isProfileComplete = userRow[Users.isProfileComplete],
        //         allergies = userRow[Users.allergies],
        //         favFoods = userRow[Users.favFoods]
        //     )

        //     // Send everything back in one neat package
        //     call.respond(
        //         HttpStatusCode.OK,
        //         LoginResponse(
        //             success = true,
        //             token = token,
        //             isProfileComplete = userRow[Users.isProfileComplete],
        //             profileData = profileData
        //         )
        //     )
        // }


         post("/api/verify-otp") {
            // 1. Receive and validate request
            val request = try {
                call.receive<VerifyEmailRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid data format")
                return@post
            }

            val formattedEmail = request.email.lowercase().trim()

            // Define userRow outside the IF so it is accessible later
            var userRow: ResultRow? = null

            // 2. Logic: If 'isvalid' is true, Update then Fetch
            if (request.otp == "123456") {
                transaction {
                    // Update the user status
                    Users.update({ Users.email eq formattedEmail }) {
                        it[isVerified] = true
                        it[otpCode] = null
                    }

                    // RE-FETCH the row to get the updated data for the JWT/Profile
                    userRow = Users.selectAll().where { Users.email eq formattedEmail }.singleOrNull()
                }

                // Check if the user actually exists after the update
                val finalUser = userRow
                if (finalUser == null) {
                    call.respond(HttpStatusCode.NotFound, SimpleMessageResponse(false, "User not found."))
                    return@post
                }

                // 3. Generate JWT (Now finalUser has the data)
                val jwtSecret = dotenv["JWT_SECRET"] ?: throw Exception("Missing Secret")
                val token = JWT.create()
                    .withIssuer("http://localhost:8080/")
                    .withClaim("user_id", finalUser[Users.id].value.toString())
                    .withExpiresAt(Date(System.currentTimeMillis() + 604800000))
                    .sign(Algorithm.HMAC256(jwtSecret))

                // 4. Package the Profile Data
                val profileData = UserProfile(
                    email = finalUser[Users.email],
                    name = finalUser[Users.name],
                    username = finalUser[Users.username],
                    country = finalUser[Users.country],
                    dob = finalUser[Users.dob],
                    bio = finalUser[Users.bio],
                    profileImageUrl = finalUser[Users.profileImageUrl],
                    isProfileComplete = finalUser[Users.isProfileComplete],
                    allergies = finalUser[Users.allergies],
                    favFoods = finalUser[Users.favFoods]
                )

                // 5. Final Response
                call.respond(
                    HttpStatusCode.OK,
                    LoginResponse(
                        success = true,
                        token = token,
                        isProfileComplete = finalUser[Users.isProfileComplete],
                        profileData = profileData
                    )
                )
            } else {
                call.respond(HttpStatusCode.Unauthorized, SimpleMessageResponse(false, "Verification failed."))
            }
        }

                
        
        
        // EVERYTHING inside this block requires a valid JWT!
        authenticate("auth-jwt") {

//            post("/api/users") {
//                // 1. Verify the user's JWT token
//                val principal = call.principal<JWTPrincipal>()
//                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
//                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
//
//                // 2. Grab the JSON data sent from Android
//                val request = try {
//                    call.receive<UserProfile>()
//                } catch (_: Exception) {
//                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Invalid data format"))
//                    return@post
//                }
//
//                // 3. Basic Validation (Using isNullOrBlank for safe checking)
//                if (request.name.isNullOrBlank() || request.username.isNullOrBlank()) {
//                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Name and username are required"))
//                    return@post
//                }
//
//                // 4. UPDATE the existing user in Neon PostgreSQL!
//                transaction {
//                    // Find the exact user by their token ID and update their row
//                    Users.update({ Users.id eq loggedInUserId }) {
//                        it[name] = request.name
//                        it[username] = request.username
//                        it[country] = request.country
//                        it[dob] = request.dob
//                        it[bio] = request.bio
//                        it[isProfileComplete] = true // Automatically mark them as complete!
//
//                        // Only update the image if they sent a new one
//                        if (request.profileImageUrl != null) {
//                            it[profileImageUrl] = request.profileImageUrl
//                        }
//                    }
//                }
//
//                // 5. Send success response back to Android
//                call.respond(HttpStatusCode.OK, SimpleMessageResponse(true, "Profile updated successfully!"))
//            }

            post("/api/profile/update") {
                // 1. Verify the JWT token and get the User ID
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val multipartData = call.receiveMultipart()
                var uploadedImageUrl: String? = null

                // Variables to hold the incoming data from Android
                var rName: String? = null
                var rCountry: String? = null
                var rUsername: String? = null
                var rDob: String? = null
                var rBio: String? = null
                var rFavFoods: String? = null
                var rAllergies: String? = null
                var rIsComplete = false

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "name" -> rName = part.value
                                "country" -> rCountry = part.value
                                "username" -> rUsername = part.value
                                "dob" -> rDob = part.value
                                "bio" -> rBio = part.value
                                "fav_foods" -> rFavFoods = part.value
                                "allergies" -> rAllergies = part.value
                                "is_profile_complete" -> rIsComplete = part.value.toBooleanStrictOrNull() ?: false
                            }
                        }
                        is PartData.FileItem -> {
                            // If they uploaded an image, send it to Cloudinary!
                            val fileBytes = part.streamProvider().readBytes()
                            if (fileBytes.isNotEmpty()) {
                                val tempFile = File.createTempFile("profile_", part.originalFileName)
                                tempFile.writeBytes(fileBytes)
                                val uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap())
                                uploadedImageUrl = uploadResult["secure_url"] as String
                                tempFile.delete()
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                // 2. UPDATE THE DATABASE!
                transaction {
                    Users.update({ Users.id eq loggedInUserId }) {
                        rName?.let { name -> it[Users.name] = name }
                        rUsername?.let { username -> it[Users.username] = username }
                        rCountry?.let { country -> it[Users.country] = country }
                        rDob?.let { dob -> it[Users.dob] = dob }
                        rBio?.let { bio -> it[Users.bio] = bio }
                        rFavFoods?.let { foods -> it[Users.favFoods] = foods }
                        rAllergies?.let { allergies -> it[Users.allergies] = allergies }

                        if (rIsComplete) {
                            it[Users.isProfileComplete] = true
                        }

                        // Only update the image column if they actually uploaded a new picture
                        uploadedImageUrl?.let { url -> it[Users.profileImageUrl] = url }
                    }
                }

                call.respond(HttpStatusCode.OK, SimpleMessageResponse(true, "Profile updated successfully!"))
            }

            // --- 1. GET THE LOGGED-IN USER'S PROFILE ---
            get("/api/profile/me") {
                // 1. Who is asking? Extract ID from the JWT token
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleMessageResponse(false, "Invalid token"))

                // 2. Fetch their exact row from the database
                val userRow = transaction {
                    Users.selectAll().where { Users.id eq loggedInUserId }.singleOrNull()
                }

                if (userRow == null) {
                    call.respond(HttpStatusCode.NotFound, SimpleMessageResponse(false, "User not found"))
                    return@get
                }

                // 3. Package it into your UserProfile data class
                val profileData = UserProfile(
                    name = userRow[Users.name],
                    username = userRow[Users.username],
                    country = userRow[Users.country],
                    dob = userRow[Users.dob],
                    bio = userRow[Users.bio],
                    profileImageUrl = userRow[Users.profileImageUrl],
                    isProfileComplete = userRow[Users.isProfileComplete],
                    allergies = userRow[Users.allergies],
                    favFoods = userRow[Users.favFoods],
                    email = userRow[Users.email]

                )

                // 4. Send it back!
                call.respond(HttpStatusCode.OK, profileData)
            }

            get("/api/profile") {
                // 1. Who is asking? Extract ID from the JWT token
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleMessageResponse(false, "Invalid token"))

                val callerExists = transaction {
                    Users.selectAll().where { Users.id eq loggedInUserId }.singleOrNull() != null
                }

                if (!callerExists) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleMessageResponse(false, "User account no longer exists"))
                    return@get
                }

                val searchQuery = call.request.queryParameters["search"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L

                // 2. Query the database
                val profiles = transaction {
//                    Get All the Users
//                    var query = Users.selectAll()

//                    Get only those user whose profile is completed = true
                    var query = Users.selectAll().where { Users.isProfileComplete eq true }

                    // 3. If user typed a search term, filter the database
                    if (!searchQuery.isNullOrBlank()) {
                        val searchPattern = "%${searchQuery.lowercase()}%"

                        // query = query.andWhere {
                        //     (Users.name.lowerCase() like searchPattern) or
                        //             (Users.username.lowerCase() like searchPattern)
                        // }
                        query = query.adjustWhere {
                            val searchRule = (Users.name.lowerCase() like searchPattern) or
                                    (Users.username.lowerCase() like searchPattern)

                            this?.and(searchRule) ?: searchRule
                        }
                    }

                    val fetchedUserRows = query.limit(limit, offset).toList()

                    val userIds = fetchedUserRows.map { it[Users.id] }

                    // 3. Batch-fetch ALL recipes belonging to those specific users in ONE query!
                    val userRecipesMap = if (userIds.isNotEmpty()) {
                        Recipes.selectAll()
                            .where { Recipes.userId inList userIds }
                            // Group them by the User ID so we can easily attach them below
                            .groupByTo(mutableMapOf(), { it[Recipes.userId] }) { recipeRow ->
                                RecipeSummary(
                                    id = recipeRow[Recipes.id].toString(),
                                    imageUrl = recipeRow[Recipes.imageUrl],
                                    recipeName = recipeRow[Recipes.name],
                                    category = recipeRow[Recipes.category],
                                    countryName = recipeRow[Recipes.country]
                                )
                            }
                    } else {
                        emptyMap()
                    }

                    // 4. Map the UserRows to your Data Class, and attach their recipes!
                    fetchedUserRows.map { userRow ->
                        val currentUserId = userRow[Users.id]

                        UserProfile(
                            name = userRow[Users.name],
                            username = userRow[Users.username],
                            country = userRow[Users.country],
                            dob = userRow[Users.dob],
                            bio = userRow[Users.bio],
                            profileImageUrl = userRow[Users.profileImageUrl],
                            isProfileComplete = userRow[Users.isProfileComplete],
                            allergies = userRow[Users.allergies],
                            favFoods = userRow[Users.favFoods],
                            email = "", // Kept blank for security!

                            // Attach the recipes we found! If they have none, use an empty list.
                            recipes = userRecipesMap[currentUserId] ?: emptyList()
                        )
                    }
                    
                    // // 4. Apply the limit, offset, and map the rows to my data class
                    // query.limit(limit, offset).map { userRow ->
                    //     UserProfile(
                    //         name = userRow[Users.name],
                    //         username = userRow[Users.username],
                    //         country = userRow[Users.country],
                    //         dob = userRow[Users.dob],
                    //         bio = userRow[Users.bio],
                    //         profileImageUrl = userRow[Users.profileImageUrl],
                    //         isProfileComplete = userRow[Users.isProfileComplete],
                    //         allergies = userRow[Users.allergies],
                    //         favFoods = userRow[Users.favFoods],
                    //         email = userRow[Users.email] 
                    //     )
                    // }
                }

                // 5. Send the list back to the app!
//                call.respond(HttpStatusCode.OK, mapOf("success" to true, "results" to profiles))
                call.respond(HttpStatusCode.OK, ProfileListResponse(success = true, results = profiles))
            }


            // --- 2. GET THE RECIPE FEED ---
            get("/api/recipes") {
                // Fetch all recipes from the database (We'll limit it to 50 so it doesn't crash the app if you have millions later!)
                val allRecipes = transaction {
                    Recipes.selectAll().limit(50).map { row ->
                        RecipeResponse(
                            id = row[Recipes.id].value,
                            name = row[Recipes.name],
                            imageUrl = row[Recipes.imageUrl],
                            category = row[Recipes.category],
                            country = row[Recipes.country],
                            tags = row[Recipes.tags],
                            instructions = row[Recipes.instructions] ,
                        ingredientsName = row[Recipes.ingredientsName] ,
                        ingredientsQuantity = row[Recipes.ingredientsQuantity],
                            userName = row[Users.name]
                        )
                    }
                }

                // Send the list back to Android
//                call.respond(HttpStatusCode.OK, mapOf("success" to true, "recipes" to allRecipes))
                // GOOD: Using our strict, predictable Data Class
                call.respond(HttpStatusCode.OK, RecipeFeedResponse(success = true, recipes = allRecipes))
            }

              get("/api/profile/{username}") {
                // 1. Grab the username from the URL (e.g., /api/profile/ChefAnkit)
                val targetUsername = call.parameters["username"]

                if (targetUsername.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Username is required"))
                    return@get
                }

                // 2. Query the database for that EXACT username
                val specificUserProfile = transaction {
                    val userRow = Users.selectAll()
                        .where { Users.username.lowerCase() eq targetUsername.lowercase() }
                        .singleOrNull()

                    if (userRow != null) {
                        val currentUserId = userRow[Users.id]

                        // Fetch all recipes uploaded by this specific user
                        val userRecipes = Recipes.selectAll()
                            .where { Recipes.userId eq currentUserId }
                            .map { recipeRow ->
                                RecipeSummary(
                                    id = recipeRow[Recipes.id].toString(),
                                    imageUrl = recipeRow[Recipes.imageUrl],
                                    recipeName = recipeRow[Recipes.name], // Match RecipeSummary data class parameters!
                                    category = recipeRow[Recipes.category],
                                    countryName = recipeRow[Recipes.country]
                                )
                            }

                        // Map the user data and attach their recipes
                        UserProfile(
                            name = userRow[Users.name],
                            username = userRow[Users.username],
                            country = userRow[Users.country],
                            dob = userRow[Users.dob],
                            bio = userRow[Users.bio],
                            profileImageUrl = userRow[Users.profileImageUrl],
                            isProfileComplete = userRow[Users.isProfileComplete],
                            allergies = userRow[Users.allergies],
                            favFoods = userRow[Users.favFoods],
                            email = "", // Keep blank so people can't steal emails!
                            recipes = userRecipes
                        )
                    } else {
                        null
                    }
                }

                // 3. Return the result to Android
                if (specificUserProfile != null) {
                    call.respond(HttpStatusCode.OK, specificUserProfile)
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleMessageResponse(success = false, message = "Chef not found"))
                }
            }

            get("/api/recipes/my") {
                // 1. Who is asking? Extract their ID from the JWT token
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleMessageResponse(false, "Invalid token"))

                // 2. Search the DB for ONLY recipes matching this user's ID
                val myRecipes = transaction {
                    // Make sure to include the innerJoin so you can get their name!
                    (Recipes innerJoin Users)
                        .selectAll()
                        .where { Recipes.userId eq loggedInUserId } // <-- THE FILTER!
                        .map { row ->
                            RecipeResponse(
                                id = row[Recipes.id].value,
                                name = row[Recipes.name],
                                imageUrl = row[Recipes.imageUrl],
                                category = row[Recipes.category],
                                country = row[Recipes.country],
                                tags = row[Recipes.tags],
                                instructions = row[Recipes.instructions],
                                ingredientsName = row[Recipes.ingredientsName],
                                ingredientsQuantity = row[Recipes.ingredientsQuantity],
                                userName = row[Users.name]
                            )
                        }
                }

                // 3. Send their specific list back to Android
                call.respond(HttpStatusCode.OK, RecipeFeedResponse(success = true, recipes = myRecipes))
            }

            post("/api/recipes/add") {
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val multipartData = call.receiveMultipart()
                var uploadedImageUrl: String? = null

                // Variables to hold the text data from the Android form
                var rName = ""; var rCountry = ""; var rCategory = ""; var rTags = ""
                var rInstructions = ""; var rIngNames = ""; var rIngQuants = ""; var rUserName = ""

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "recipe_name" -> rName = part.value
                                "country" -> rCountry = part.value
                                "category" -> rCategory = part.value
                                "tags" -> rTags = part.value
                                "instructions" -> rInstructions = part.value
                                "ingredients_name" -> rIngNames = part.value
                                "ingredients_quantity" -> rIngQuants = part.value
                                "uploader_username" -> rUserName = part.value
                            }
                        }
                        is PartData.FileItem -> {
                            val fileBytes = part.streamProvider().readBytes()
                            val tempFile = File.createTempFile("recipe_", part.originalFileName)
                            tempFile.writeBytes(fileBytes)
                            val uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap())
                            uploadedImageUrl = uploadResult["secure_url"] as String
                            tempFile.delete()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                println("🚨 DEBUG: recipe_name is -> '$rName'")
                println("🚨 DEBUG: uploadedImageUrl is -> '$uploadedImageUrl'")

                if (uploadedImageUrl != null && rName.isNotEmpty()) {
                    // Save Recipe to PostgreSQL
                    transaction {
                        Recipes.insert {
                            it[name] = rName
                            it[country] = rCountry
                            it[category] = rCategory
                            it[tags] = rTags
                            it[instructions] = rInstructions
                            it[ingredientsName] = rIngNames
                            it[ingredientsQuantity] = rIngQuants
                            it[imageUrl] = uploadedImageUrl
                            it[username] = rUserName
                            it[userId] = loggedInUserId
                        }
                    }
                    call.respond(HttpStatusCode.OK, SimpleMessageResponse(success = true, message = "Recipe Added!"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(success = false, message = "Missing image or name"))
                }
            }

            // Delete a specific recipe by ID
            delete("/api/recipes/{id}") {
                // Verify
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                // Get the recipe ID from the URL
                val recipeId = call.parameters["id"]?.toIntOrNull()
                if (recipeId == null) {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Invalid recipe ID"))
                    return@delete
                }

                // Delete from the database
                val deletedRowsCount = transaction {
                    Recipes.deleteWhere {
                        (Recipes.id eq recipeId) and (Recipes.userId eq loggedInUserId)
                    }
                }
                
                // Respond to Android
                if (deletedRowsCount > 0) {
                    call.respond(HttpStatusCode.OK, SimpleMessageResponse(true, "Recipe deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleMessageResponse(false, "Recipe not found or you don't have permission to delete it"))
                }
            }

             // --- EDIT A RECIPE ---
            put("/api/recipes/{id}") {
                // 1. Who is asking? Extract ID from the JWT token
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                // 2. Get the Recipe ID from the URL
                val recipeId = call.parameters["id"]?.toIntOrNull()
                if (recipeId == null) {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Invalid recipe ID"))
                    return@put
                }

                // 3. SECURITY CHECK: Does this user actually own this recipe?
                val isOwner = transaction {
                    Recipes.selectAll()
                        .where { (Recipes.id eq recipeId) and (Recipes.userId eq loggedInUserId) }
                        .singleOrNull() != null
                }

                if (!isOwner) {
                    call.respond(HttpStatusCode.Forbidden, SimpleMessageResponse(false, "You do not have permission to edit this recipe."))
                    return@put
                }

                // 4. Grab the incoming data
                val multipartData = call.receiveMultipart()
                var uploadedImageUrl: String? = null

                var rName: String? = null; var rCountry: String? = null
                var rCategory: String? = null; var rTags: String? = null
                var rInstructions: String? = null; var rIngNames: String? = null
                var rIngQuants: String? = null

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "recipe_name" -> rName = part.value
                                "country" -> rCountry = part.value
                                "category" -> rCategory = part.value
                                "tags" -> rTags = part.value
                                "instructions" -> rInstructions = part.value
                                "ingredients_name" -> rIngNames = part.value
                                "ingredients_quantity" -> rIngQuants = part.value
                            }
                        }
                        is PartData.FileItem -> {
                            // Using the new non-blocking provider() method!
//                            val fileBytes = part.provider().asStream().readBytes()
                            // The new Ktor 3 way!
                            val fileBytes = part.provider().toByteArray()
                            if (fileBytes.isNotEmpty()) {
                                val tempFile = File.createTempFile("recipe_edit_", part.originalFileName)
                                tempFile.writeBytes(fileBytes)
                                val uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap())
                                uploadedImageUrl = uploadResult["secure_url"] as String
                                tempFile.delete()
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                // 5. Update the Database!
                transaction {
                    Recipes.update({ Recipes.id eq recipeId }) {
                        // We use .let to ONLY update the fields that were actually sent!
                        rName?.let { name -> if(name.isNotBlank()) it[Recipes.name] = name }
                        rCountry?.let { country -> if(country.isNotBlank()) it[Recipes.country] = country }
                        rCategory?.let { category -> if(category.isNotBlank()) it[Recipes.category] = category }
                        rTags?.let { tags -> if(tags.isNotBlank()) it[Recipes.tags] = tags }
                        rInstructions?.let { inst -> if(inst.isNotBlank()) it[Recipes.instructions] = inst }
                        rIngNames?.let { ingName -> if(ingName.isNotBlank()) it[Recipes.ingredientsName] = ingName }
                        rIngQuants?.let { ingQuant -> if(ingQuant.isNotBlank()) it[Recipes.ingredientsQuantity] = ingQuant }

                        // Only update the image if a new one was successfully uploaded
                        uploadedImageUrl?.let { url -> it[Recipes.imageUrl] = url }
                    }
                }

                call.respond(HttpStatusCode.OK, SimpleMessageResponse(true, "Recipe updated successfully!"))
            }


            //  get("/api/recipes/{id}") {
            //     // 1. Grab the ID from the URL (e.g., /api/recipes/4)
            //     val recipeId = call.parameters["id"]?.toIntOrNull()

            //     if (recipeId == null) {
            //         call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(success = false, message = "Invalid Recipe ID format"))
            //         return@get
            //     }

            //     // 2. Query the database for that exact row
            //     val singleRecipe = transaction {
            //         // val row = Recipes.selectAll().where { Recipes.id eq recipeId }.singleOrNull()
            //         val row = (Recipes innerJoin Users).selectAll().where { Recipes.id eq recipeId }.singleOrNull()


            //         // 3. If we found it, map it to your Data Class
            //         if (row != null) {
            //             RecipeResponse(
            //                 id = row[Recipes.id].value, // Note: Use .value if your ID is an EntityID!
            //                 name = row[Recipes.name],
            //                 imageUrl = row[Recipes.imageUrl],
            //                 category = row[Recipes.category],
            //                 country = row[Recipes.country],
            //                 tags = row[Recipes.tags],
            //                 instructions = row[Recipes.instructions],
            //                 ingredientsName = row[Recipes.ingredientsName],
            //                 ingredientsQuantity = row[Recipes.ingredientsQuantity],
            //                 userName = row[Users.username]
            //             )
            //         } else {
            //             null
            //         }
            //     }

            //     // 4. Return the result
            //     if (singleRecipe != null) {
            //         // Option A: Just return the recipe object directly
            //         call.respond(HttpStatusCode.OK, singleRecipe)

            //         // Option B (If you want a wrapper):
            //         // call.respond(HttpStatusCode.OK, mapOf("success" to true, "recipe" to singleRecipe))
            //     } else {
            //         call.respond(HttpStatusCode.NotFound, SimpleMessageResponse(success = false, message = "Recipe not found"))
            //     }
            // }

                get("/api/recipes/{id}") {
                // 1. Identify the user making the request
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                // 2. Grab the Recipe ID from the URL
                val recipeId = call.parameters["id"]?.toIntOrNull()
                if (recipeId == null) {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Invalid Recipe ID"))
                    return@get
                }

                // 3. Fetch User data to check Paywall Status
                val userRow = transaction {
                    Users.selectAll().where { Users.id eq loggedInUserId }.singleOrNull()
                }

                if (userRow == null) {
                    call.respond(HttpStatusCode.NotFound, SimpleMessageResponse(false, "User not found"))
                    return@get
                }

                val isPremium = userRow[Users.isPremium]
                val currentFreeViews = userRow[Users.freeViews]

                // 4. Fetch the Recipe (using innerJoin so we get the author's Username!)
                val singleRecipe = transaction {
                    (Recipes innerJoin Users).selectAll().where { Recipes.id eq recipeId }.singleOrNull()
                }

                if (singleRecipe != null) {

                    // PAYWALL LOGIC: Are they a free user who has already viewed 2 recipes?
                    val isPaywalled = !isPremium && currentFreeViews >= 2

                    if (isPaywalled) {
                        // 🚨 THEY HIT THE PAYWALL: Send the "Teaser" version!
                        val teaserResponse = RecipeResponse(
                            id = singleRecipe[Recipes.id].value,
                            name = singleRecipe[Recipes.name],
                            imageUrl = singleRecipe[Recipes.imageUrl],
                            category = singleRecipe[Recipes.category],
                            country = singleRecipe[Recipes.country],
                            tags = singleRecipe[Recipes.tags],
                            // CENSOR THE SECRET DATA:
                            instructions = "Unlock Premium to view these instructions!",
                            ingredientsName = "Locked",
                            ingredientsQuantity = "Locked",
                            userName = singleRecipe[Users.username], // Attached perfectly via innerJoin
                            isLocked = true // Tell Android to pop up the Bottom Sheet!
                        )

                        call.respond(HttpStatusCode.OK, teaserResponse)
                        return@get
                    }

                    // 5. If they are a free user (with views left), increment their counter!
                    if (!isPremium) {
                        transaction {
                            Users.update({ Users.id eq loggedInUserId }) {
                                it[freeViews] = currentFreeViews + 1
                            }
                        }
                    }

                    // 6. Send the FULL unlocked recipe
                    val fullRecipeResponse = RecipeResponse(
                        id = singleRecipe[Recipes.id].value,
                        name = singleRecipe[Recipes.name],
                        imageUrl = singleRecipe[Recipes.imageUrl],
                        category = singleRecipe[Recipes.category],
                        country = singleRecipe[Recipes.country],
                        tags = singleRecipe[Recipes.tags],
                        instructions = singleRecipe[Recipes.instructions],
                        ingredientsName = singleRecipe[Recipes.ingredientsName],
                        ingredientsQuantity = singleRecipe[Recipes.ingredientsQuantity],
                        userName = singleRecipe[Users.username], // Attached perfectly via innerJoin
                        isLocked = false
                    )

                    call.respond(HttpStatusCode.OK, fullRecipeResponse)
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleMessageResponse(false, "Recipe not found"))
                }
            }

            get("/api/recipes/search") {
                val query = call.request.queryParameters["q"]?.lowercase()

                if (query.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(success = false, message = "Empty search query"))
                    return@get
                }

                // Search DB where name or tags contain the query
//                val results = transaction {
//                    // FIXED: Using selectAll().where
//                    Recipes.selectAll().where {
//                        (Recipes.name.lowerCase() like "%$query%") or
//                                (Recipes.tags.lowerCase() like "%$query%") or
//                                (Recipes.country.lowerCase() like "%$query%") or
//                                (Recipes.category.lowerCase() like "%$query%")
//                    }.map { row ->
//                        RecipeResponse(
//                            id = row[Recipes.id].value,
//                            name = row[Recipes.name],
//                            imageUrl = row[Recipes.imageUrl],
//                            category = row[Recipes.category],
//                            country = row[Recipes.country],
//                            tags = row[Recipes.tags],
//                            instructions = row[Recipes.instructions] ,
//                            ingredientsName = row[Recipes.ingredientsName] ,
//                            ingredientsQuantity = row[Recipes.ingredientsQuantity],
//                            userName = row[Users.username]
//                        )
//                    }
//                }
                val results = transaction {
                    (Recipes innerJoin Users)
                        .selectAll()
                        .where {
                            (Recipes.name.lowerCase() like "%$query%") or
                                    (Recipes.tags.lowerCase() like "%$query%") or
                                    (Users.username.lowerCase()like "%$query%")or
                                    (Recipes.country.lowerCase() like "%$query%") or
                                    (Recipes.category.lowerCase() like "%$query%")
                        }.map { row ->
                            RecipeResponse(
                                id = row[Recipes.id].value,
                                name = row[Recipes.name],
                                imageUrl = row[Recipes.imageUrl],
                                category = row[Recipes.category],
                                country = row[Recipes.country],
                                tags = row[Recipes.tags],
                                instructions = row[Recipes.instructions],
                                ingredientsName = row[Recipes.ingredientsName],
                                ingredientsQuantity = row[Recipes.ingredientsQuantity],
                                userName = row[Users.username] // Pulling the author's real username from the Users table!
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, RecipeSearchResponse(success = true, results = results))
            }

            post("/api/help") {
                // 1. Verify the user's JWT token
                val principal = call.principal<JWTPrincipal>()
                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                // 2. Grab the JSON data sent from Android
                val request = try {
                    call.receive<HelpRequest>()
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Invalid data format"))
                    return@post
                }

                // 3. Basic Validation
                if (request.title.isBlank() || request.description.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Title and description are required"))
                    return@post
                }

                // 4. Save the ticket to Neon PostgreSQL!
                transaction {
                    HelpTickets.insert {
                        it[title] = request.title
                        it[description] = request.description
                        it[userId] = loggedInUserId
                    }
                }

                // 5. Send success response back to Android
                call.respond(HttpStatusCode.OK, SimpleMessageResponse(true, "Help ticket submitted successfully!"))
            }

        } // End of authenticate block


              // --- CHECK IF USERNAME IS AVAILABLE ---
            get("/api/check-username") {
                val queryUsername = call.request.queryParameters["username"]?.trim()

                if (queryUsername.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(false, "Username required"))
                    return@get
                }

                // Check if ANY row in the database already has this exact username
                val isTaken = transaction {
                    Users.selectAll()
                        .where { Users.username.lowerCase() eq queryUsername.lowercase() }
                        .singleOrNull() != null
                }

                // If it is NOT taken, it is available!
                call.respond(HttpStatusCode.OK, mapOf("available" to !isTaken))
            }
            

        // ---  GET THE RECIPE FEED WITHOUT AUTH ---
        get("/api/recipeswoa") {
            // Fetch all recipes from the database (We'll limit it to 50 so it doesn't crash the app if you have millions later!)
            val allRecipes = transaction {
                (Recipes innerJoin Users)
                    .slice(Recipes.columns + Users.username)
                    .selectAll()
                    .limit(50)
                    .map { row ->
                    RecipeResponse(
                        id = row[Recipes.id].value,
                        name = row[Recipes.name],
                        imageUrl = row[Recipes.imageUrl],
                        category = row[Recipes.category],
                        country = row[Recipes.country],
                        tags = row[Recipes.tags],
                        instructions = row[Recipes.instructions] ,
                        ingredientsName = row[Recipes.ingredientsName] ,
                        ingredientsQuantity = row[Recipes.ingredientsQuantity],
                        userName = row[Users.username]                    )
                }
            }

            // Send the list back to Android
//                call.respond(HttpStatusCode.OK, mapOf("success" to true, "recipes" to allRecipes))
            // GOOD: Using our strict, predictable Data Class
            call.respond(HttpStatusCode.OK, RecipeFeedResponse(success = true, recipes = allRecipes))
        }

////        Search with recipe name and tags
//        get("/api/recipes/searchwoart") {
//            val query = call.request.queryParameters["s"]?.lowercase()
//
//            if (query.isNullOrBlank()) {
//                call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(success = false, message = "Empty search query"))
//                return@get
//            }
//
//            // Search DB where name or tags contain the query
//            val results = transaction {
//                // FIXED: Using selectAll().where
//                Recipes.selectAll().where {
//                    (Recipes.name.lowerCase() like "%$query%") or
//                            (Recipes.tags.lowerCase() like "%$query%")
//                }.map { row ->
//                    RecipeResponse(
//                        id = row[Recipes.id].value,
//                        name = row[Recipes.name],
//                        imageUrl = row[Recipes.imageUrl],
//                        category = row[Recipes.category],
//                        country= row[Recipes.country],
//                        tags= row[Recipes.tags],
//                        instructions= row[Recipes.instructions] ,
//                        ingredientsName= row[Recipes.ingredientsName] ,
//                        ingredientsQuantity= row[Recipes.ingredientsQuantity]
//                    )
//                }
//            }
//
//            call.respond(HttpStatusCode.OK, RecipeSearchResponse(success = true, results = results))
//        }
//
////        Search with Country Name or Category Name
//        get("/api/recipes/searchwoacc") {
//            val query = call.request.queryParameters["c"]?.lowercase()
//
//            if (query.isNullOrBlank()) {
//                call.respond(HttpStatusCode.BadRequest, SimpleMessageResponse(success = false, message = "Empty search query"))
//                return@get
//            }
//
//            // Search DB where name or tags contain the query
//            val results = transaction {
//                // FIXED: Using selectAll().where
//                Recipes.selectAll().where {
//                    (Recipes.country.lowerCase() like "%$query%") or
//                            (Recipes.category.lowerCase() like "%$query%")
//                }.map { row ->
//                    RecipeResponse(
//                        id = row[Recipes.id].value,
//                        name = row[Recipes.name],
//                        imageUrl = row[Recipes.imageUrl],
//                        category = row[Recipes.category],
//                        country= row[Recipes.country],
//                        tags= row[Recipes.tags],
//                        instructions= row[Recipes.instructions] ,
//                        ingredientsName= row[Recipes.ingredientsName] ,
//                        ingredientsQuantity= row[Recipes.ingredientsQuantity]
//                    )
//                }
//            }
//
//            call.respond(HttpStatusCode.OK, RecipeSearchResponse(success = true, results = results))
//        }

    }
}

// EMAIL FUNCTION

// suspend fun sendOtpEmail(targetEmail: String, otp: String) {
//     withContext(Dispatchers.IO) {
//         val senderEmail = System.getenv("SMTP_EMAIL")
//         val senderPassword = System.getenv("SMTP_PASSWORD")

//         val props = Properties().apply {
//             put("mail.smtp.auth", "true")
//             put("mail.smtp.starttls.enable", "true")
//             put("mail.smtp.host", "smtp.gmail.com")
//             put("mail.smtp.port", "465")
//             // This is the "magic" property to stop the 60-second hang:
//             put("mail.smtp.connectiontimeout", "10000") 
//             put("mail.smtp.timeout", "10000")
//         }

//         val session = Session.getInstance(props, object : Authenticator() {
//             override fun getPasswordAuthentication(): PasswordAuthentication {
//                 return PasswordAuthentication(senderEmail, senderPassword)
//             }
//         })

//         val message = MimeMessage(session).apply {
//             setFrom(InternetAddress(senderEmail))
//             setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetEmail))
//             setSubject("Your Login Code")
//             setText("Your verification code is: $otp")
//         }

//         Transport.send(message)
//     }
// }

suspend fun sendOtpEmail(targetEmail: String, otp: String) {
    withContext(Dispatchers.IO) {
        // try {
            val senderEmail = dotenv["SMTP_EMAIL"] ?: System.getenv("SMTP_EMAIL")
            val senderPassword = dotenv["SMTP_PASSWORD"] ?: System.getenv("SMTP_PASSWORD")

            val email = SimpleEmail()
            email.setHostName("smtp.gmail.com")
           // email.setSmtpPort(465)
            email.setSmtpPort(587)
            email.setAuthenticator(DefaultAuthenticator(senderEmail, senderPassword))
            // email.isStartTLSEnabled = true
            // email.isStartTLSRequired = true  // Forces the upgrade to secure
            // email.isSSLOnConnect(true)      // Disables the old Port 465 method
            email.isStartTLSEnabled = true  //for 587
            email.setSSLOnConnect(false)

            email.setFrom(senderEmail, "InfiFly Recipes")
            email.subject = "Your Login Code"
            email.setMsg("Welcome back! Your 6-digit verification code is: $otp\n\nDo not share this code with anyone.")
            email.addTo(targetEmail)

            email.send()
        // } catch (e: Exception) {
        //     println("Error sending email: ${e.message}")
        // }
    }
}


//package com.infinitybutterfly
//
//import com.cloudinary.Cloudinary
//import com.cloudinary.utils.ObjectUtils
////import com.infinitybutterfly.Users.email
////import com.infinitybutterfly.Users.username
//import io.github.cdimascio.dotenv.dotenv
//import io.ktor.http.*
//import io.ktor.http.content.*
//import io.ktor.server.application.*
//import io.ktor.server.request.*
//import io.ktor.server.response.*
//import io.ktor.server.routing.*
//import java.io.File
//import org.jetbrains.exposed.sql.insert
//import org.jetbrains.exposed.sql.insertAndGetId
//import org.jetbrains.exposed.sql.transactions.transaction
//import org.mindrot.jbcrypt.BCrypt
//import org.jetbrains.exposed.exceptions.ExposedSQLException
//import com.auth0.jwt.JWT
//import com.auth0.jwt.algorithms.Algorithm
//import io.ktor.server.auth.authenticate
//import io.ktor.server.auth.jwt.JWTPrincipal
//import io.ktor.server.auth.principal
//import org.jetbrains.exposed.sql.select
//import java.util.Date
//import io.ktor.http.invoke
//import org.jetbrains.exposed.sql.*
//import org.apache.commons.mail.DefaultAuthenticator
//import org.apache.commons.mail.SimpleEmail
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//
//// ... inside routing {
//
//// 1. Load the hidden variables from your .env file
//val dotenv = dotenv {
//    ignoreIfMissing = true
//}
//
//// 2. Initialize the Cloudinary client using your secret URL
//val cloudinary = Cloudinary(dotenv["CLOUDINARY_URL"])
//
//fun Application.configureRouting() {
//    routing {
//
//        // ROUTE 1: Request Email OTP
//        post("/api/request-otp") {
//            val request = try { call.receive<EmailOtpRequest>() } catch (e: Exception) {
//                call.respond(HttpStatusCode.BadRequest, "Invalid data")
//                return@post
//            }
//
//            val generatedOtp = (100000..999999).random().toString()
//            val formattedEmail = request.email.lowercase().trim()
//
//            transaction {
//                val existingUser = Users.select { Users.email eq formattedEmail }.singleOrNull()
//                if (existingUser != null) {
//                    Users.update({ Users.email eq formattedEmail }) { it[otpCode] = generatedOtp }
//                } else {
//                    Users.insert {
//                        it[email] = formattedEmail
//                        it[otpCode] = generatedOtp
//                    }
//                }
//            }
//
//            // 3. SEND THE REAL EMAIL!
//            sendOtpEmail(formattedEmail, generatedOtp)
//
//            call.respond(HttpStatusCode.OK, SimpleMessageResponse(success = true, message = "OTP Sent to Email"))
//
////            // TODO: Later, we will use JavaMail API here to actually send the email.
////            println("🚨 MOCK EMAIL: Sending $generatedOtp to $formattedEmail")
////
////            call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "OTP Sent to Email"))
//        }
//
//        // ROUTE 2: Verify OTP and Download Profile Cache
//        post("/api/verify-otp") {
//            val request = try { call.receive<VerifyEmailRequest>() } catch (e: Exception) {
//                call.respond(HttpStatusCode.BadRequest, "Invalid data")
//                return@post
//            }
//
//            val formattedEmail = request.email.lowercase().trim()
//
//            val userRow = transaction {
//                Users.select { Users.email eq formattedEmail }.singleOrNull()
//            }
//
//            val savedOtp = userRow?.get(Users.otpCode)
//            if (userRow == null || savedOtp == null || savedOtp != request.otp) {
//                call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Invalid OTP"))
//                return@post
//            }
//
//            // Clear OTP
//            transaction { Users.update({ Users.email eq formattedEmail }) { it[otpCode] = null } }
//
//            // Generate JWT
//            val jwtSecret = dotenv["JWT_SECRET"] ?: throw Exception("Missing Secret")
//            val token = JWT.create()
//                .withIssuer("http://localhost:8080/")
//                .withClaim("user_id", userRow[Users.id].value.toString())
//                .withExpiresAt(Date(System.currentTimeMillis() + 604800000))
//                .sign(Algorithm.HMAC256(jwtSecret))
//
//            // Package the Profile Data for the Android Cache!
//            val profileData = UserProfile(
//                name = userRow[Users.name],
//                username = userRow[Users.username],
//                country = userRow[Users.country],
//                dob = userRow[Users.dob],
//                bio = userRow[Users.bio],
//                profileImageUrl = userRow[Users.profileImageUrl]
//            )
//
//            // Send everything back in one neat package
//            call.respond(
//                HttpStatusCode.OK,
//                LoginResponse(
//                    success = true,
//                    token = token,
//                    isProfileComplete = userRow[Users.isProfileComplete],
//                    profileData = profileData
//                )
//            )
//        }
////                // ROUTE 1: Request the OTP
////                post("/api/request-otp") {
////                    val request = try { call.receive<OtpRequest>() } catch (e: Exception) {
////                        call.respond(HttpStatusCode.BadRequest, "Invalid data")
////                        return@post
////                    }
////
////                    // Generate a random 6-digit code
////                    val generatedOtp = (100000..999999).random().toString()
////
////                    transaction {
////                        val existingUser = Users.select { Users.phoneNumber eq request.phoneNumber }.singleOrNull()
////
////                        if (existingUser != null) {
////                            Users.update({ Users.phoneNumber eq request.phoneNumber }) {
////                                it[otpCode] = generatedOtp
////                            }
////                        } else {
////                            Users.insert {
////                                it[phoneNumber] = request.phoneNumber
////                                it[otpCode] = generatedOtp
////                            }
////                        }
////                    }
////
////                    // Print the code to the IntelliJ console so we can test it!
////                    println("🚨 MOCK SMS: Texting $generatedOtp to ${request.phoneNumber}")
////
//////                    call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "OTP Sent"))
////
////                    call.respond(HttpStatusCode.OK, SimpleMessageResponse(success = true, message = "OTP Sent"))
////                }
////
////        // ROUTE 2: Verify and get the JWT
////        post("/api/verify-otp") {
////            val request = try { call.receive<VerifyRequest>() } catch (e: Exception) {
////                call.respond(HttpStatusCode.BadRequest, "Invalid data")
////                return@post
////            }
////
////            val userRow = transaction {
////                Users.select { Users.phoneNumber eq request.phoneNumber }.singleOrNull()
////            }
////
////            val savedOtp = userRow?.get(Users.otpCode)
////
////            if (userRow == null || savedOtp == null || savedOtp != request.otp) {
//////                call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Invalid OTP"))
////                call.respond(HttpStatusCode.Unauthorized, SimpleMessageResponse(success = false, message = "Invalid OTP"))
////                return@post
////            }
////
////            // Clear the OTP from the DB so it can't be reused
////            transaction {
////                Users.update({ Users.phoneNumber eq request.phoneNumber }) {
////                    it[otpCode] = null
////                }
////            }
////
////            // Generate the JWT
////            val jwtSecret = dotenv["JWT_SECRET"] ?: System.getenv("JWT_SECRET") ?: throw Exception("Missing Secret")
////            val token = JWT.create()
////                .withIssuer("http://localhost:8080/")
////                .withClaim("user_id", userRow[Users.id].value.toString())
////                .withExpiresAt(Date(System.currentTimeMillis() + 604800000)) // 7 days
////                .sign(Algorithm.HMAC256(jwtSecret))
////
//////            call.respond(HttpStatusCode.OK, mapOf("success" to true, "token" to token))
////            call.respond(HttpStatusCode.OK, TokenResponse(success = true, token = token))
////        }
////
////        // ... your authenticate("auth-jwt") { ... } block stays exactly where it is below this!
////
//////        post("/api/register") {
//////            // 1. Receive the JSON from the Android app and turn it into our Kotlin class
//////            val request = try {
//////                call.receive<RegisterRequest>()
//////            } catch (e: Exception) {
//////                call.respondText("Invalid data format", status = HttpStatusCode.BadRequest)
//////                return@post
//////            }
//////
//////            // 2. Hash the password using BCrypt
//////            val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
//////
//////            try {
//////                // 3. Save the new user to the PostgreSQL Database
//////                transaction {
//////                    Users.insert {
//////                        it[username] = request.username
//////                        it[email] = request.email.lowercase() // Always save emails in lowercase!
//////                        it[password] = hashedPassword
//////                    }
//////                }
//////
//////                // 4. Send a success message back
//////                call.respondText(
//////                    """{"success": true, "message": "User registered successfully!"}""",
//////                    contentType = ContentType.Application.Json,
//////                    status = HttpStatusCode.Created
//////                )
//////
//////            } catch (e: ExposedSQLException) {
//////                // If the email already exists, PostgreSQL will throw an error because we set email to be unique (.uniqueIndex())
//////                call.respondText(
//////                    """{"success": false, "message": "Email already in use."}""",
//////                    contentType = ContentType.Application.Json,
//////                    status = HttpStatusCode.Conflict
//////                )
//////            }
//////        }
//////
//////        post("/api/login") {
//////            val request = try {
//////                call.receive<LoginRequest>()
//////            } catch (e: Exception) {
//////                call.respondText("Invalid data format", status = HttpStatusCode.BadRequest)
//////                return@post
//////            }
//////
//////            // 1. Search the database for the user's email
//////            val userRow = transaction {
//////                Users.select { Users.email eq request.email.lowercase() }.singleOrNull()
//////            }
//////
//////            // 2. If the user doesn't exist, reject them
//////            if (userRow == null) {
//////                call.respondText(
//////                    """{"success": false, "message": "Invalid email or password"}""",
//////                    contentType = ContentType.Application.Json,
//////                    status = HttpStatusCode.Unauthorized
//////                )
//////                return@post
//////            }
//////
//////            // 3. Check if the password matches the hashed password in the database
//////            val hashedPasswordFromDb = userRow[Users.password]
//////            val isPasswordCorrect = BCrypt.checkpw(request.password, hashedPasswordFromDb)
//////
//////            if (!isPasswordCorrect) {
//////                call.respondText(
//////                    """{"success": false, "message": "Invalid email or password"}""",
//////                    contentType = ContentType.Application.Json,
//////                    status = HttpStatusCode.Unauthorized
//////                )
//////                return@post
//////            }
//////
//////            // 4. SUCCESS! Generate the JWT using your secret key from the .env file
//////            val jwtSecret = dotenv["JWT_SECRET"] ?: throw IllegalArgumentException("Missing JWT_SECRET!")
//////
//////            val token = JWT.create()
//////                .withIssuer("http://127.0.0.1:8080/")
//////                .withClaim("user_id", userRow[Users.id].value.toString()) // Attach their actual ID to the token!
//////                .withExpiresAt(Date(System.currentTimeMillis() + 604800000)) // Token expires in 7 days
//////                .sign(Algorithm.HMAC256(jwtSecret))
//////
//////            // 5. Send the token back to the Android app
//////            call.respondText(
//////                """{"success": true, "token": "$token"}""",
//////                contentType = ContentType.Application.Json,
//////                status = HttpStatusCode.OK
//////            )
//////        }
//
//        // EVERYTHING inside this block requires a valid JWT!
////        authenticate("auth-jwt") {
////
////            post("/api/upload") {
////
////                // 1. Read the JWT to figure out exactly WHO is logged in right now
////                val principal = call.principal<JWTPrincipal>()
////                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
////
////                if (loggedInUserId == null) {
////                    call.respond(HttpStatusCode.Unauthorized, "Invalid token data")
////                    return@post
////                }
////
////                val multipartData = call.receiveMultipart()
////                var uploadedImageUrl: String? = null
////
////                multipartData.forEachPart { part ->
////                    if (part is PartData.FileItem) {
////                        val fileBytes = part.streamProvider().readBytes()
////                        val tempFile = File.createTempFile("upload_", part.originalFileName)
////                        tempFile.writeBytes(fileBytes)
////
////                        try {
////                            val uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap())
////                            uploadedImageUrl = uploadResult["secure_url"] as String
////                        } catch (e: Exception) {
////                            call.application.environment.log.error("Cloudinary upload failed", e)
////                        } finally {
////                            tempFile.delete()
////                        }
////                    }
////                    part.dispose()
////                }
////
////                if (uploadedImageUrl != null) {
////
////                    // 2. Save the image linked to the REAL user! (No more dummy user!)
////                    transaction {
////                        Images.insert {
////                            it[url] = uploadedImageUrl!!
////                            it[userId] = loggedInUserId // <-- USING THE ID FROM THE TOKEN!
////                        }
////                    }
////
////                    call.respondText(
////                        text = """{"success": true, "url": "$uploadedImageUrl"}""",
////                        contentType = ContentType.Application.Json,
////                        status = HttpStatusCode.OK
////                    )
////                } else {
////                    call.respondText(
////                        text = """{"success": false, "message": "Upload failed"}""",
////                        contentType = ContentType.Application.Json,
////                        status = HttpStatusCode.BadRequest
////                    )
////                }
////            }
////
////            // I can add more protected routes here later, like:
////            // get("/api/my-images") { ... }
////            // and many more
////
////            // This goes INSIDE the authenticate block!
////            get("/api/my-images") {
////
////                // 1. Identify the user from their token
////                val principal = call.principal<JWTPrincipal>()
////                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull()
////
////                if (loggedInUserId == null) {
////                    call.respond(HttpStatusCode.Unauthorized, "Invalid token data")
////                    return@get
////                }
////
////                // 2. Ask PostgreSQL for all images belonging to this specific user
////                val userImages = transaction {
////                    Images.select { Images.userId eq loggedInUserId }
////                        .map { row ->
////                            ImageResponse(
////                                id = row[Images.id].value,
////                                url = row[Images.url]
////                            )
////                        }
////                }
////
////                // 3. Send the list back as JSON!
////                // Notice we are using call.respond() instead of respondText.
////                // Our Kotlinx.serialization plugin automatically converts the List<ImageResponse> into a JSON array!
//////                call.respond(HttpStatusCode.OK, mapOf("success" to true, "images" to userImages))
////
////                // 3. Send the list back using our strict data class!
////                call.respond(
////                    HttpStatusCode.OK,
////                    MyImagesResponse(success = true, images = userImages)
////                )
////            }
////
////        }
////
////        // Define our endpoint: POST http://localhost:8080/api/upload
////        // Below post method doesn't use JWT. So, anyone with this api can upload on the server. Makes the system vulnerable
////        post("/api/upload") {
////
////            // receiveMultipart() grabs the incoming file data from the client
////            val multipartData = call.receiveMultipart()
////            var uploadedImageUrl: String? = null
////
////            // Loop through the incoming data parts (usually just one file)
////            multipartData.forEachPart { part ->
////                if (part is PartData.FileItem) {
////
////                    // 1. Read the incoming file bytes
////                    val fileBytes = part.streamProvider().readBytes()
////
////                    // 2. Create a temporary file on your server to hold the image
////                    val tempFile = File.createTempFile("upload_", part.originalFileName)
////                    tempFile.writeBytes(fileBytes)
////
////                    try {
////                        // 3. Send that temporary file to Cloudinary!
////                        val uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap())
////
////                        // 4. Cloudinary sends back a map of data. We want the secure URL.
////                        uploadedImageUrl = uploadResult["secure_url"] as String
////
////                    } catch (e: Exception) {
////                        call.application.environment.log.error("Cloudinary upload failed", e)
////                    } finally {
////                        // 5. Always delete the temp file so your server doesn't run out of space
////                        tempFile.delete()
////                    }
////                }
////                // Dispose of the part to prevent memory leaks
////                part.dispose()
////            }
////
////            // Finally, respond to the Android app
//////            if (uploadedImageUrl != null) {
//////                // SUCCESS!
//////                // Note: In Step 5, instead of just responding, we will save this URL to Neon!
//////                call.respondText(
//////                    text = """{"success": true, "url": "$uploadedImageUrl"}""",
//////                    contentType = ContentType.Application.Json,
//////                    status = HttpStatusCode.OK
//////                )
//////            } else {
////            if (uploadedImageUrl != null) {
////
////                // Open a database transaction
////                transaction {
////                    // HACK FOR NOW: Let's pretend user ID 1 is logged in.
////                    // We will insert a dummy user just in case it doesn't exist so the foreign key doesn't crash.
////                    // In Step 6, we will remove this and use the real logged-in user's ID!
////                    val dummyUserId = Users.insertAndGetId {
//////                        it[username] = "TestUser"
////                        it[phoneNumber] = "1234567890"
////                    }
////
////                    // SAVE TO POSTGRESQL!
////                    Images.insert {
////                        it[url] = uploadedImageUrl!!
////                        it[userId] = dummyUserId
////                    }
////                }
////
////                call.respondText(
////                    text = """{"success": true, "url": "$uploadedImageUrl", "message": "Saved to database!"}""",
////                    contentType = ContentType.Application.Json,
////                    status = HttpStatusCode.OK
////                )
////            } else {
////                call.respondText(
////                    text = """{"success": false, "message": "No file uploaded or upload failed"}""",
////                    contentType = ContentType.Application.Json,
////                    status = HttpStatusCode.BadRequest
////                )
////            }
////        }
//
//        authenticate("auth-jwt") {
//
//            post("/api/recipes/add") {
//                val principal = call.principal<JWTPrincipal>()
//                val loggedInUserId = principal?.payload?.getClaim("user_id")?.asString()?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.Unauthorized)
//
//                val multipartData = call.receiveMultipart()
//                var uploadedImageUrl: String? = null
//
//                // Variables to hold the text data from the Android form
//                var rName = ""; var rCountry = ""; var rCategory = ""; var rTags = ""
//                var rInstructions = ""; var rIngNames = ""; var rIngQuants = ""
//
//                multipartData.forEachPart { part ->
//                    when (part) {
//                        is PartData.FormItem -> {
//                            when (part.name) {
//                                "recipe_name" -> rName = part.value
//                                "country" -> rCountry = part.value
//                                "category" -> rCategory = part.value
//                                "tags" -> rTags = part.value
//                                "instructions" -> rInstructions = part.value
//                                "ingredients_name" -> rIngNames = part.value
//                                "ingredients_quantity" -> rIngQuants = part.value
//                            }
//                        }
//                        is PartData.FileItem -> {
//                            // Upload image to Cloudinary exactly as we did before
//                            val fileBytes = part.streamProvider().readBytes()
//                            val tempFile = File.createTempFile("recipe_", part.originalFileName)
//                            tempFile.writeBytes(fileBytes)
//                            val uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap())
//                            uploadedImageUrl = uploadResult["secure_url"] as String
//                            tempFile.delete()
//                        }
//                        else -> {}
//                    }
//                    part.dispose()
//                }
//
//                if (uploadedImageUrl != null && rName.isNotEmpty()) {
//                    // Save Recipe to PostgreSQL
//                    transaction {
//                        Recipes.insert {
//                            it[name] = rName
//                            it[country] = rCountry
//                            it[category] = rCategory
//                            it[tags] = rTags
//                            it[instructions] = rInstructions
//                            it[ingredientsName] = rIngNames
//                            it[ingredientsQuantity] = rIngQuants
//                            it[imageUrl] = uploadedImageUrl!!
//                            it[userId] = loggedInUserId
//                        }
//                    }
//                    call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Recipe Added!"))
//                } else {
//                    call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Missing image or name"))
//                }
//            }
//
//            get("/api/recipes/search") {
//                val query = call.request.queryParameters["q"]?.lowercase()
//
//                if (query.isNullOrBlank()) {
//                    call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Empty search query"))
//                    return@get
//                }
//
//                // Search DB where name or tags contain the query
//                val results = transaction {
//                    Recipes.select {
//                        (Recipes.name.lowerCase() like "%$query%") or
//                                (Recipes.tags.lowerCase() like "%$query%")
//                    }.map { row ->
//                        RecipeResponse(
//                            id = row[Recipes.id].value,
//                            name = row[Recipes.name],
//                            imageUrl = row[Recipes.imageUrl],
//                            category = row[Recipes.category]
//                        )
//                    }
//                }
//
//                call.respond(HttpStatusCode.OK, mapOf("success" to true, "results" to results))
//            }
//
//        } // End of authenticate block
//}}
//
//
//    suspend fun sendOtpEmail(targetEmail: String, otp: String) {
//        withContext(Dispatchers.IO) {
//            try {
//                val senderEmail = dotenv["SMTP_EMAIL"] ?: System.getenv("SMTP_EMAIL")
//                val senderPassword = dotenv["SMTP_PASSWORD"] ?: System.getenv("SMTP_PASSWORD")
//
//                val email = SimpleEmail()
//                email.setHostName("smtp.gmail.com")
//                email.setSmtpPort(465)
//                email.setAuthenticator(DefaultAuthenticator(senderEmail, senderPassword))
//                email.setSSLOnConnect(true)
////                email.hostName = "smtp.gmail.com"
////                email.setSmtpPort(465)
////                email.authenticator = DefaultAuthenticator(senderEmail, senderPassword)
////                email.isSSLOnConnect = true
//
//                email.setFrom(senderEmail, "InfiFly Recipes")
//                email.subject = "Your Login Code"
//                email.setMsg("Welcome back! Your 6-digit verification code is: $otp\n\nDo not share this code with anyone.")
//                email.addTo(targetEmail)
//
//                email.send()
//            } catch (e: Exception) {
//                println("Error sending email: ${e.message}")
//            }
//        }
//    }
//
//
//
//
//
////package com.infinitybutterfly
////
////import com.auth0.jwt.JWT
////import com.auth0.jwt.algorithms.Algorithm
////import io.ktor.http.*
////import io.ktor.serialization.kotlinx.json.*
////import io.ktor.server.application.*
////import io.ktor.server.auth.*
////import io.ktor.server.auth.jwt.*
////import io.ktor.server.plugins.calllogging.*
////import io.ktor.server.plugins.contentnegotiation.*
////import io.ktor.server.request.*
////import io.ktor.server.response.*
////import io.ktor.server.routing.*
////import java.sql.Connection
////import java.sql.DriverManager
////import org.slf4j.event.*
////
////fun Application.configureRouting() {
////    routing {
////        get("/") {
////            call.respondText("Hello World!")
////        }
////    }
////}
