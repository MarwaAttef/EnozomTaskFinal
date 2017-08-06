# EnozomTaskFinal

Firebase(Storage,Database),Access Gallery(Images,Video),Camera

Splash screen{ 
-check (camera,Read_External_storage) permissions 
-wait 3 seconds }

MainActivity{
 -Check network connection 
-access gallery(images,videos),Camera 
-firebase storage upload\delete 
-firebase database }

libs{ compile 'com.android.support:appcompat-v7:25.3.1' 
compile 'com.android.support:design:25.3.1' 
compile 'com.google.firebase:firebase-storage:11.0.0' 
compile 'com.google.firebase:firebase-auth:11.0.0' 
compile 'com.google.firebase:firebase-database:11.0.0' }
