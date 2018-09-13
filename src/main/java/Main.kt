import com.squareup.moshi.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@JsonClass(generateAdapter = true)
data class Box<T>(
		val value: T
)
typealias GoodBox = Box<String>
typealias NullBox = Box<Nothing?>
typealias BadBox = Box<Nothing>

// as would be used when writing code for retrofit
interface Service {
	fun bad(box: BadBox)
	fun good(box: GoodBox)
	fun nullBox(box: NullBox)
}

fun main(args: Array<String>) {

	val moshi = Moshi.Builder()
			// this adapter is not relevant since the error happens while loading the generated
			// Box adapter, i.e. before any adapter for the value in Box is loaded
			.add(object : Any() {
				@ToJson fun toJson(writer: JsonWriter, o: Nothing?) {
					writer.nullValue()
				}

				@FromJson
				fun fromJson(reader: JsonReader): Nothing? {
					reader.skipValue()
					return null
				}
			})
			.build()

	// this is how retrofit gets the types (see https://github.com/square/retrofit/blob/c1b100f3c4e84711fa23bdfed03cb78c4d8a8f19/retrofit/src/main/java/retrofit2/RequestFactory.java#L145)
	fun getType(name: String) = Service::class.java.methods.find { it.name == name }!!.genericParameterTypes.first()

	val badType: Type = getType("bad")
	val goodType: Type = getType("good")
	val nullType: Type = getType("nullBox")

	// this is okay, the generated adapter is loaded
	moshi.adapter<GoodBox>(goodType)

	// this works too, as the type is correctly parameterized
	moshi.adapter<BadBox>(object : ParameterizedType {
		override fun getRawType(): Type = Box::class.java
		override fun getOwnerType(): Type? = null
		override fun getActualTypeArguments(): Array<Type> = arrayOf(Nothing::class.java)
	})

	// these fail because they are raw types
	moshi.adapter<NullBox>(nullType)
	moshi.adapter<BadBox>(badType)

}
