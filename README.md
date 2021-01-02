# LogOS
Logical Operating System

This is a hobby project in which I aim at creating a conversational machine (chatbot). Hopefully one day it will be able to hold a simple
conversation. For now it is just an experimental repository where I throw different ideas in.

There are two kinds of approaches that I considered: symbolic AI and a simple pattern matching algorithm with a huge amount of hardcoded
responses to user inputs. So far I've noticed that natural language has so many exceptions from rules that it seems impossible to write
an elegant algorithm that describes the whole variety of possible sentences without losing individual meaning.

I packed symbolic approaches and NLP in `logos` and simple chatbot in `bot`. The latter doesn't even need any external libraries (yet).
