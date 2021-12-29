# Java sources for the Street Legal Racing: Redline
It's a public Git repo where we could make the game better together. You can get the game [on Steam](https://store.steampowered.com/app/497180/Street_Legal_Racing_Redline_v231/).

## What's included?
Currently it's only **sl/scripts/game/**, but more folders might be added in the future.

## Why does the game need these javas?
**Street Legal Racing: Redline** is using a modified version of the Sun's Java virtual machine and compiles external sourcefiles into classes on startup. The concept of adding this virtual machine is to allow anyone outside development team to modify the gameplay, add cars, parts or other objects.

## How to deploy these sources in my game?
Create a copy of your game folder somewhere, then download all sources from Git repo and paste into this copied folder. The game looks into **src** folder to find a corresponding **.java** file for each compiled class. You can add your own class as well, just create new **.java** file and render it according to the internal coding standard.

## How do I run my modified javas in the game?
Just modify any existing **.java** file, save it and run the game, it will check modification dates for all sourcefiles and if there's something new, game will try to compile the source into a class. Once compilation is successful, you'll find a corresponding **.class** file.

## This virtual machine is different from what I've discovered in Java books. How do I learn this version of the Java?
Overall it's pretty similar to the original Sun's version, but it has some specific limitations and currently the best way to understand it is to learn yourself by trial and error.

## What does a 'native' keyword mean?
The **native** keyword tells virtual machine that an object has a native implementation, i.e. it's just hardcoded in the exe and you can't change its behavior.

## I've done something wrong and the game is crashing now. How to fix that?
First of all, check your game folder and find an **error.log** file there. You'll have 2 possible scenarios:
1. An **error.log** exists - most likely, your problem is trivial and explained in this file comprehensively enough. Try reverting your changes to make the game work, then use logic and common sense to understand what could cause the issue.
2. Game didn't generate **error.log** file - you've entered some black hole area and what you've done is so weird that the game doesn't even have an explanation for it. Check your code, find any odd looking lines, remove or change them to get a different response from the game.

## Typical errors in the error.log and what they mean
1. **syntax error** - obviously, you're violating internal coding standard, look into other files to learn more about it. If it seems difficult, learn about the original Sun's Java from books first.
2. **unknown field** - you're about to access a property that is not declared or does not exist at all. For example, for a **cuteFluffyCat.horn** there's no such property as **horn**, have you seen a rhino cat somewhere?
3. **[illegal methodcall] null.something()** - you're trying to call **something()** on an object that doesn't exist or not properly initialized. It's like to pick a steering wheel without a car and try to drive somewhere. People running around with steering wheels are looking strange and the game thinks so as well.
