# LogOS
Logical Operating System

This is a hobby project in which I aim at creating a conversational machine (chatbot). Hopefully one day it will be able to hold a simple
conversation. For now it is just an experimental repository where I throw different ideas in (see repo LogosBot as well).

There are two kinds of approaches that I considered: symbolic AI and a simple pattern matching algorithm with a huge amount of hardcoded
responses to user inputs. So far I've noticed that natural language has so many exceptions from rules that it seems impossible to write
an elegant algorithm that describes the whole variety of possible sentences without losing "soul" or individual meaning.

I packed symbolic approaches and NLP in `logos` and simple chatbot in `bot`. The latter doesn't even need any external libraries (yet).

As for now I prefer the personal chatbot approach that gives a very good possibility to express myself. That's exactly what I need from
this project. I don't want to create yet another "soulless" utility maximizer or anything like that. Our world has a lot of that stuff
already.

But the simple chatbot isn't totally stupid - it's not just a dictionary of responses. The class `StateVariables` will contain a whole
bunch of data fields that should describe internal and external states. This will allow a primitive context understanding and coreference.

My strategy will be just adding at least 10 patterns a day to the huge `Logos` class. Let's see where this will take me.
