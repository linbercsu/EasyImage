# EasyImage

Easy to display image into ImageView on Android. 

```kotlin
EasyImage
         .with(activity)
         .load("your url")
         .placeholder(R.drawable.loading)
         .error(R.drawable.error)
         .fadeIn()
         .rounded(20f)
         .into(imageView)
```
