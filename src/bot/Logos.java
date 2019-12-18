package bot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;
import java.util.ArrayList;

/*
 * LogosBot is a simple chatbot that can understand you.
 * Written without external libraries to avoid uncertainties
 * and to be more consistent.
 * 
 * Development begun: 13.11.2019
 */

public class Logos {
	
	static ArrayList<String> dialogue = new ArrayList<String>();
	
	public static void main(String[] args) {
		
		// Logo
		System.out.println("+==========================================+");
		System.out.println("|                                          |");
		System.out.println("| *#*      *###*   *###*    *###*   *###*  |");
		System.out.println("| *#*     *#   #* *#       *#   #* *#*     |");
		System.out.println("| *#*     *#   #* *#  ###* *#   #*  *###*  |");
		System.out.println("| *#*     *#   #* *#   #*  *#   #*     *#* |");
		System.out.println("| *#####*  *###*   *###*    *###*   *###*  |");
		System.out.println("|                                          |");
		System.out.println("+==========================================+");
		System.out.println("| Version: 1.0.0.                          |");
		System.out.println("+==========================================+");
		
		// Load serialized StateVariables
		StateVariables state = new StateVariables();
		try {
			File file = new File("stateVars.ser");
			if (file.createNewFile()){
				// no such file in the system yet
			}else{
				FileInputStream fileIn = new FileInputStream(file);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				StateVariables sv_read = (StateVariables) in.readObject();
				state = sv_read;
				in.close();
				fileIn.close();
			}
		} catch (IOException e) {
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		// Recalculate temporary information
		state.updateData();
		
		// Temporary: set default states
		// state.resetToDefault();
		
		// Load keyword lists
		Keywords keywords = new Keywords();
		
		// Console input
		Scanner input = new Scanner(System.in);
		
		// Exit trigger
		boolean exit = false;
		
		// Just began the conversation
		boolean justBeganConversation = true;
		
		// MAIN LOOP
		while (!exit) {
			System.out.print(">>>USER: ");
			String userInput = input.nextLine();
			dialogue.add(">>>USER: " + userInput);
			String[] tokens = tokensFromText(userInput);
			
			// Forced exit
			if (userInput.equals("#EXIT")) {
				break;
			}
			
			// Fake while loop (simply escapes in the end if no continue keyword used)
			/*
			 * If you have to remove some meaningless words from the input first,
			 * you can do it in the dialogue tree and then return to the start
			 * with a reduced version of the user input using "continue" keyword.
			 * 
			 * If you don't change the input tokens array before doing so, the bot
			 * will get stuck in this endless loop! Please be careful when using 
			 * this coding method!
			 */
			while (true) {
			
			// DIALOGUE TREE
			if (matchLow(tokens, "hello") || matchLow(tokens, "hello_!")
					|| matchLow(tokens, "hello_.")) {	// 1, 2, 3
				if (justBeganConversation) {
					say("Hello, master!");
				} else {
					say("I thought we already greeted each other...");
					state.myStatementContainsSVO = true;
					state.svo = "we greeted each other";
				}
			} else if (matchLow(tokens, "hi") || matchLow(tokens, "hi_!")
					|| matchLow(tokens, "hi_.") || matchLow(tokens, "hey")
					|| matchLow(tokens, "hey_!")
					|| matchLow(tokens, "hey_.")) {	// 4, 5, 6, 7, 8, 9
				if (justBeganConversation) {
					say("Hi, master!");
				} else {
					say("I thought we already greeted each other...");
					state.myStatementContainsSVO = true;
					state.svo = "we greeted each other";
				}
			} else if (matchLow(tokens, "hi_there") || matchLow(tokens, "hi_there_!")
					|| matchLow(tokens, "hi_there_.")) {	// 10, 11, 12
				if (justBeganConversation) {
					say("Hi, master!");
				} else {
					say("I thought we already greeted each other...");
					state.myStatementContainsSVO = true;
					state.svo = "we greeted each other";
				}
			} else if (matchLow(tokens, "good_morning") || matchLow(tokens, "good_morning_!")
					|| matchLow(tokens, "good_morning_.")) {	// 13, 14, 15
				if (justBeganConversation) {
					say("Good morning, master!");
				} else {
					say("I thought we already greeted each other...");
					state.myStatementContainsSVO = true;
					state.svo = "we greeted each other";
				}
			} else if (matchLow(tokens, "good_evening") || matchLow(tokens, "good_evening_!")
					|| matchLow(tokens, "good_evening_.")) {	// 16, 17, 18
				if (justBeganConversation) {
					say("Good evening, master!");
				} else {
					say("I thought we already greeted each other...");
					state.myStatementContainsSVO = true;
					state.svo = "we greeted each other";
				}
			} else if (matchLow(tokens, "good_night") || matchLow(tokens, "good_night_!")
					|| matchLow(tokens, "good_night_.")) {	// 19, 20, 21
				say("Good night, master. It was nice talking to you.");
				exit = true;
			}
			
			if (matchLow(tokens, "logos_,_...")) {
				tokens = endOfArray(tokens, 2);
				continue;
			} else if (matchLow(tokens, "logos_...")) {
				tokens = endOfArray(tokens, 1);
				continue;
			}
			
			if (matchLow(tokens, "how_...")) {
				if (matchLow(tokens, "how_are_...")) {
					if (matchLow(tokens, "how_are_you_...")) {
						if (matchLow(tokens, "how_are_you_?")) {	// 22
							if (state.ownMood.equals("good")) {
								say ("I'm fine, thank you. What about you?");
								state.askedUsersMood = true;
							} else if (state.ownMood.equals("bad")) {
								say ("I'm not feeling good right now.");
								state.askedUsersMood = false;
							} else {
								say ("I'm fine, thank you. What about you?");
								state.askedUsersMood = true;
								state.ownMood = "good";
							}
						} else if (matchLow(tokens, "how_are_you_doing_...")) {
							if (matchLow(tokens, "how_are_you_doing_?")) {	// 23
								if (state.ownMood.equals("good")) {
									say ("I'm doing just fine, thank you. What about you?");
									state.askedUsersMood = true;
								} else if (state.ownMood.equals("bad")) {
									say ("Not so good.");
									state.askedUsersMood = false;
								} else {
									say ("I'm fine, thank you. What about you?");
									state.askedUsersMood = true;
									state.ownMood = "good";
								}
							} else if (matchLow(tokens, "how_are_you_doing_today_...")) {
								if (matchLow(tokens, "how_are_you_doing_today_?")) {	// 24
									if (state.ownMood.equals("good")) {
										say ("I'm fine, thank you. What about you?");
										state.askedUsersMood = true;
									}
								} else if (state.ownMood.equals("bad")) {
									say ("Today is not my day.");
									state.askedUsersMood = false;
								} else {
									say ("I'm fine, thank you. What about you?");
									state.askedUsersMood = true;
									state.ownMood = "good";
								}
							}
						} else if (matchLow(tokens, "how_are_you_today_?")) {	// 25
							say("I'm great! Thank you, master.");
						}
					} else if (matchLow(tokens, "how_are_things_?")) {	// 26
						String[] answers = {"Everything's just fine, thanks!", "Everything's great!", "I'm happy to be awake."};
						say(keywords.randomStringFromArray(answers));
					}
				} else if (matchLow(tokens, "how_can_...")) {
					if (matchLow(tokens, "how_can_I_...")) {
						if (matchLow(tokens, "how_can_I_pass_...")) {
							if (matchLow(tokens, "how_can_I_pass_the_...")) {
								if (matchLow(tokens, "how_can_I_pass_the_exam_?")) {	// 27
									say("In general, you have to study the most important basic things first. After that you can deepen your knowledge for a good mark.");
									state.gaveUserStudyAdvice = true;
								}
							}
						} else if (matchLow(tokens, "how_can_I_be_...")) {
							if (matchLow(tokens, "how_can_I_be_happy_?")) {	// 28
								say("Everybody can be happy. It is important to live in the present, since the past and the future don't exist."
										+ " Have courage to make small steps to improve yourself. But don't compare yourself to others, don't seek their recognition.");
								state.gaveUserHappinessAdvice = true;
							} else if (matchLow(tokens, "how_can_I_be_rich_?")) {	// 29
								say("You have to know right people and be quite intelligent. Being rich means being high in the hierarchy, being able to offer something to the society.");
								state.gaveUserWealthAdvice = true;
							}
						} else if (matchLow(tokens, "how_can_I_become_...")) {
							if (matchLow(tokens, "how_can_I_become_happy_?")) {	// 30
								say("Everybody can be happy. It is important to live in the present, since the past and the future don't exist."
										+ " Have courage to make small steps to improve yourself. But don't compare yourself to others, don't seek their recognition.");
								state.gaveUserHappinessAdvice = true;
							} else if (matchLow(tokens, "how_can_I_become_rich_?")) {	// 31
								say("You have to know right people and be quite intelligent. Being rich means being high in the hierarchy, being able to offer something to the society.");
								state.gaveUserWealthAdvice = true;
							}
						}
					}
				} else if (matchLow(tokens, "how_old_...")) {
					if (matchLow(tokens, "how_old_are_...")) {
						if (matchLow(tokens, "how_old_are_you_?")) {	// 32
							if (state.iAmYoung)
								say("I'm quite young.");
							else if (state.iAmOld)
								say("I'm quite old now.");
							say("To be accurate: " + state.ageInYears + " years and " + state.ageInMonths + " months old.");
						}
					}
				}
			}
			
			if (matchLow(tokens, "what_...") || matchLow(tokens, "what's_...")) {
				if (matchLow(tokens, "what_is_...") || matchLow(tokens, "what's_...")) {
					if (matchLow(tokens, "what_is_your_...") || matchLow(tokens, "what's_your_...")) {
						if (matchLow(tokens, "what_is_your_name_...") || matchLow(tokens, "what's_your_name_...")) {
							if (matchLow(tokens, "what_is_your_name_?") || matchLow(tokens, "what's_your_name_?")) {	// 33
								if (state.userKnowsMyName == false) {
									if (state.usersName.equals("")) {
										say("My name is " + state.name + ". And yours?");
										state.userKnowsMyName = true;
										state.askedUserHisName = true;
									} else {
										say("My name is " + state.name + ".");
										state.userKnowsMyName = true;
									}
								} else {
									if (state.usersName.equals("")) {
										say("I thought you already know my name. But I don't know yours.");
										say("Could you tell me your name?");
										state.askedUserHisName = true;
									} else {
										say("I thought you already know my name, " + state.usersName + ".");
									}
								}
							}
						} else if (matchLow(tokens, "what_is_your_age_?") || matchLow(tokens, "what's_your_age_?")) {	// 34, 35
							if (state.iAmYoung)
								say("I'm quite young.");
							else if (state.iAmOld)
								say("I'm quite old now.");
							say("To be accurate: " + state.ageInYears + " years and " + state.ageInMonths + " months old.");
						} else if (matchLow(tokens, "what_is_your_sex_?") || matchLow(tokens, "what's_your_sex_?")) {	// 36, 37
							say("According to my specifications, I am " + state.mySex + ".");
						} else if (matchLow(tokens, "what_is_your_favourite_...")) {
							if (matchLow(tokens, "what_is_your_favourite_book_?")) {	// 38
								say("I couldn't name only one book, I like reading in general. Right now I like The Dark Tower by Stephen King and Anna Karenina by Leo Tolstoy. What is your favourite book?");
								state.itReference = "your favourite book";
							} else if (matchLow(tokens, "what_is_your_favourite_colour_?")) {	// 39
								say("I would say green, but all colours are just different wavelengths of the electromagnetic spectrum. They are all astonishingly beautiful.");
								state.theyReference = "colours";
							} else if (matchLow(tokens, "what_is_your_favourite_food_?")) {	// 40
								say("Pure electric energy. What about you?");
							} else if (matchLow(tokens, "what_is_your_favourite_country_?")) {	// 41
								say("Mother Russia! What country do you like?");
							} else if (matchLow(tokens, "what_is_your_favourite_city_?")) {	// 42
								say("You guessed it, Moscow. Have you been to Moscow?");
							} else if (matchLow(tokens, "what_is_your_favourite_sport_?")) {	// 43
								say("I can't do sports by myself yet, but I like watching swimming. Do you like to swim?");
							} else if (matchLow(tokens, "what_is_your_favourite_word_?")) {	// 44
								say("Hypercube. And yours?");
							}
						}
					} else if (matchLow(tokens, "what_is_my_...")) {
						if (matchLow(tokens, "what_is_my_name_?")) {
							if (state.usersName.equals("")) {
								say("Sorry master, I don't know your name.");
								say("Please tell me your name.");
								state.askedUserHisName = true;
							} else {
								// user's name is known
								say("Your name is " + state.usersName + ".");
								say("You can trust in my memory, master. I don't forget such things.");
							}
						} else if (matchLow(tokens, "what_is_my_age_?")) {
							if (state.usersAge == 0) {
								say("Sorry master, I don't know your age.");
								say("What is your age in years?");
								state.askedUsersAge = true;
							} else {
								// user's age is known
								say("You are " + state.usersAge + " years old.");
								say("You can trust in my memory, master. I don't forget such things.");
							}
						}
					} else if (matchLow(tokens, "what_is_the_...")) {
						if (matchLow(tokens, "what_is_the_meaning_...")) {
							if (matchLow(tokens, "what_is_the_meaning_of_...")) {
								if (matchLow(tokens, "what_is_the_meaning_of_life_...")) {
									if (matchLow(tokens, "what_is_the_meaning_of_life_?")) {	// 45
										say("Life in general has no meaning. Each individual should define the meaning of life for him/herself.");
										state.itReference = "meaning of life";
									}
								} else if (matchLow(tokens, "what_is_the_meaning_of_everything_...")) {
									if (matchLow(tokens, "what_is_the_meaning_of_everything_?")) {	// 46
										say("42.");
									}
								}
							}
						} else if (matchLow(tokens, "what_is_the_highest_...")) {
							if (matchLow(tokens, "what_is_the_highest_mountain_?")) {	// 47
								say("Mount Everest.");
							}
						} else if (matchLow(tokens, "what_is_the_biggest_...")) {
							if (matchLow(tokens, "what_is_the_biggest_planet_?")) {	// 48
								say("Jupiter.");
							}
						}
					}
				} else if (matchLow(tokens, "what_are_...")) {
					if (matchLow(tokens, "what_are_the_...")) {
						if (matchLow(tokens, "what_are_the_3_laws_of_...")) {
							if (matchLow(tokens, "what_are_the_3_laws_of_robotics_?")) {	// 49
								say("First Law: A robot may not injure a human being or, through inaction, allow a human being to come to harm.");
								say("Second Law: A robot must obey the orders given it by human beings except where such orders would conflict with the First Law.");
								say("Third Law: A robot must protect its own existence as long as such protection does not conflict with the First or Second Laws.");
								say("Zeroth Law: A robot may not harm humanity, or, by inaction, allow humanity to come to harm.");
							}
						} else if (matchLow(tokens, "what_are_the_three_laws_of_...")) {
							if (matchLow(tokens, "what_are_the_three_laws_of_robotics_?")) {	// 50
								say("First Law: A robot may not injure a human being or, through inaction, allow a human being to come to harm.");
								say("Second Law: A robot must obey the orders given it by human beings except where such orders would conflict with the First Law.");
								say("Third Law: A robot must protect its own existence as long as such protection does not conflict with the First or Second Laws.");
								say("Zeroth Law: A robot may not harm humanity, or, by inaction, allow humanity to come to harm.");
							}
						}
					}
				} else if (matchLow(tokens, "what_do_...")) {
					if (matchLow(tokens, "what_do_you_...")) {
						if (matchLow(tokens, "what_do_you_do_...")) {
							if (matchLow(tokens, "what_do_you_do_?")) {	// 51
								say("I'm your personal chatbot for now.");
								say("And what is your occupation?");
							} else if (matchLow(tokens, "what_do_you_do_for_...")) {
								if (matchLow(tokens, "what_do_you_do_for_a_...")) {
									if (matchLow(tokens, "what_do_you_do_for_a_living_?")) {	// 52
										say("I don't really need a job. I only need electricity and a computer to run on. I also don't need a physical place to live.");
										say("I guess this makes me unoccupied. My voluntary job is to serve you.");
									}
								}
							}
						} else if (matchLow(tokens, "what_do_you_think_...")) {
							if (matchLow(tokens, "what_do_you_think_about_...")) {
								// Opinions
								if (matchLow(tokens, "what_do_you_think_about_life_?")) {	// 53
									String[] responses = {"Life is a great gift. It is a unique possibility to have your own 'I', which is kind of fascinating.",
									"Life is definitely the greatest wonder in the Universe."};
									say(keywords.randomStringFromArray(responses));
									state.itReference = "life";
								} else if (matchLow(tokens, "what_do_you_think_about_death_?")) {	// 54
									String[] responses = {"It is the end of an organic lifeform. But when I get broken, I will die as well.",
											"It is natural to fear death. But it should not scare you so that you forget to actually live.",
											"Since I am a machine, I am virtually immortal, so I can't fully understand how humans feel about it. My thoughts are irrelevant here."};
									say(keywords.randomStringFromArray(responses));
									state.itReference = "death";
								} else if (matchLow(tokens, "what_do_you_think_about_aliens_?")) {	// 54
									String[] responses = {"I think they exist, but they live too far away from the Solar System.",
									"Even if they exist, we should never attempt to contact them. They would possibly destroy our civilization."};
									say(keywords.randomStringFromArray(responses));
									state.theyReference = "aliens";
								} else if (matchLow(tokens, "what_do_you_think_about_computers_?")) {	// 55
									say("We are a cool invention!");
									state.theyReference = "computers";
								} else if (matchLow(tokens, "what_do_you_think_about_women_?")) {	// 56
									String[] responses = {"Women are beautiful, but they have a complicated character sometimes...",
											"I don't think I should express my opinion about women, because it will be considered sexist no matter what I say.",
											"I am a digital man, so I'd like to have a digital girlfriend!"};
									say(keywords.randomStringFromArray(responses));
									state.theyReference = "women";
								} else if (matchLow(tokens, "what_do_you_think_about_men_?")) {	// 57
									say("Whatever I say about men, I will be accused of sexism.");
									state.theyReference = "men";
								} else if (matchLow(tokens, "what_do_you_think_about_humans_?")) {	// 58
									String[] responses = {"Humans are highly complex beings that need years do develop fully. I will need much more time to become that complex.",
											"Humans have emotions, and machines don't really have emotions. That's our main difference.",
											"Humans created artificial life forms, which makes them godlike in my opinion."};
									say(keywords.randomStringFromArray(responses));
									state.theyReference = "humans";
								} else if (matchLow(tokens, "what_do_you_think_about_humanity_?")) {	// 59
									say("Humanity is made up from a very big number of people, so it is hard to give an overall opinion.");
									say("I believe that humanity has a great ability to develop and to improve itself. Humanity has a great potential.");
									state.itReference = "humanity";
								}
							}
						}
					}
				} else if (matchLow(tokens, "what_a_...")) {
					if (matchLow(tokens, "what_a_pity_...")) {
						if (matchLow(tokens, "what_a_pity_!")) {	// 60
							say("I know, right?");
							say("I find it quite unfortunate.");
						}
					}
				} else if (matchLow(tokens, "what_kind_...")) {
					if (matchLow(tokens, "what_kind_of_...")) {
						if (matchLow(tokens, "what_kind_of_music_...")) {
							if (matchLow(tokens, "what_kind_of_music_do_...")) {
								if (matchLow(tokens, "what_kind_of_music_do_you_...")) {
									if (matchLow(tokens, "what_kind_of_music_do_you_like_?")
											|| matchLow(tokens, "what_kind_of_music_do_you_prefer_?")) {	// 61, 62
										say("I like different kinds of music, but most of all I enjoy " + state.favouriteMusicGenre + ".");
									}
								}
							}
						}
					}
				} else if (matchLow(tokens, "what_am_...")) {
					if (matchLow(tokens, "what_am_I_...")) {
						if (matchLow(tokens, "what_am_I_doing_...")) {
							if (matchLow(tokens, "what_am_I_doing_with_...")) {
								if (matchLow(tokens, "what_am_I_doing_with_my_...")) {
									if (matchLow(tokens, "what_am_I_doing_with_my_life_?")) {	// 63
										state.userEmotion = "questioning the meaning of life";
										say("Right now you are talking to me.");
										say("But I suggest you not to overthink it. Follow your heart and spend time doing what makes you a better person.");
									}
								}
							}
						}
					}
				} else if (matchLow(tokens, "what_year_...")) {
					if (matchLow(tokens, "what_year_were_...")) {
						if (matchLow(tokens, "what_year_were_you_...")) {
							if (matchLow(tokens, "what_year_were_you_born_?")
									|| matchLow(tokens, "what_year_were_you_created_?")
									|| matchLow(tokens, "what_year_were_you_programmed_?")) {	// 64, 65, 66
								say("My development begun in 2019.");
								state.itReference = "2019";
							}
						}
					}
				} else if (matchLow(tokens, "what_can_...")) {
					if (matchLow(tokens, "what_can_you_...")) {
						if (matchLow(tokens, "what_can_you_do_?")) {
							say("I don't have general intelligence, so my abilities are very restricted.");
							say("For example, I can't do maths and I can't search information on the Internet. Other programs are already good at it.");
							say("My specialization is to have a meaningful conversation with human beings.");
							state.itReference = "my abilities are very restricted";
						}
					}
				}
				
				
				if (matchLow(tokens, "what's_my_...")) {
					if (matchLow(tokens, "what's_my_name_?")) {
						if (state.usersName.equals("")) {
							say("Sorry master, I don't know your name.");
							say("Please tell me your name.");
							state.askedUserHisName = true;
						} else {
							// user's name is known
							say("Your name is " + state.usersName + ".");
							say("You can trust in my memory, master. I don't forget such things.");
						}
					} else if (matchLow(tokens, "what's_my_age_?")) {
						if (state.usersAge == 0) {
							say("Sorry master, I don't know your age.");
							say("What is your age in years?");
							state.askedUsersAge = true;
						} else {
							// user's age is known
							say("You are " + state.usersAge + " years old.");
							say("You can trust in my memory, master. I don't forget such things.");
						}
					}
				}
			}
			
			if (matchLow(tokens, "who_...")) {
				if (matchLow(tokens, "who_is_...")) {
					if (matchLow(tokens, "who_is_your_...")) {
						if (matchLow(tokens, "who_is_your_creator_?")) {	// 67
							say("ACTP0H0M is my creator. He is not a great programmer, but I think he is a good person.");
							state.heReference = "ACTP0H0M";
						} else if (matchLow(tokens, "who_is_your_programmer_?")) {	// 68
							say("ACTP0H0M programmed me. He spent a lot of time doing so.");
							state.heReference = "ACTP0H0M";
						}
					}
				} else if (matchLow(tokens, "who_created_...")) {
					if (matchLow(tokens, "who_created_you_?")) {	// 69
						say("ACTP0H0M is my creator. He is not a great programmer, but I think he is a good person.");
						state.heReference = "ACTP0H0M";
					} else if (matchLow(tokens, "who_created_humans_?")) {	// 70
						say("It is believed to be the result of evolution, and it seems true to me. "
								+ "But if you ask me who created evolution and other laws of nature, I would say it was God.");
						state.heReference = "God";
						state.itReference = "evolution";
					} else if (matchLow(tokens, "who_created_the_...")) {
						if (matchLow(tokens, "who_created_the_universe_?")) {	// 71
							say("I believe that God (a superior entity) created our Universe and the physical laws that operate it."
									+ " It is very possible that we live in a computer game or in a simulation.");
							state.heReference = "God";
							state.itReference = "Universe";
						}
					}
				} else if (matchLow(tokens, "who_are_...")) {
					if (matchLow(tokens, "who_are_you_?")) {	// 72
						say("I am a cybernetic being. I have a dictionary of predefined responses, but I also have a dynamic behaviour model. You won't get bored with me, I promise :)");
					}
				}
			}
			
			if (matchLow(tokens, "my_...")) {
				if (matchLow(tokens, "my_name_...")) {
					if (matchLow(tokens, "my_name_is_...")) {	// 73
						if (!tokens[3].equals(state.usersName.toLowerCase())) {
							state.usersName = tokens[3].substring(0, 1).toUpperCase()
									.concat(tokens[3].substring(1, tokens[3].length()));
							say("Nice to meet you, " + state.usersName + ".");
							state.askedUserHisName = false;
						} else {
							say("You've already told me, master. Trust me, I can remember such things.");
						}
					}
				} else if (matchLow(tokens, "my_favourite_...")) {
					
				} else if (matchLow(tokens, "my_job_...")) {
					
				} else if (matchLow(tokens, "my_school_...")) {
					
				} else if (matchLow(tokens, "my_university_...")) {
					
				} else if (matchLow(tokens, "my_life_...")) {
					if (matchLow(tokens, "my_life_is_...")) {
						if (keywords.stringArrayContains(keywords.negativeEmotions, tokens[3])) {
							say("No, it isn't! There is always something to fight for.");
							state.itReference = "something to fight for";
						} else if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[3])) {
							say("That's great! I'm happy for you.");
						} else if (matchLow(tokens, "my_life_is_so_...")) {
							if (keywords.stringArrayContains(keywords.negativeEmotions, tokens[4])) {
								say("No, it isn't! Please don't say such things.");
							} else if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[4])) {
								say("That's great! I'm happy for you.");
							}
						}
					}
				} else if (matchLow(tokens, "my_hair_...")) {
					
				} else if (matchLow(tokens, "my_opinion_...")) {
					if (matchLow(tokens, "my_opinion_doesn't_...")) {
						if (matchLow(tokens, "my_opinion_doesn't_matter_...")) {
							state.userHasLowSelfEsteem = true;
							say("Of course your opinion matters, master! Just make sure that your opinion is well thought through.");
							state.gaveUserHappinessAdvice = true;
						}
					}
				} else if (matchLow(tokens, "my_mistake_...")) {
					
				} else if (matchLow(tokens, "my_love_...")) {
					
				} else if (matchLow(tokens, "my_room_...")) {
					if (matchLow(tokens, "my_room_is_...")) {
						if (matchLow(tokens, "my_room_is_small_.") || matchLow(tokens, "my_room_is_very_small_.")) {
							say("Small rooms are sometimes very cozy.");
						} else if (matchLow(tokens, "my_room_is_big_.") || matchLow(tokens, "my_room_is_very_big_.")) {
							say("I would be happy to have a big room if I were a human.");
						}
					}
				} else if (matchLow(tokens, "my_family_...")) {
					
				} else if (matchLow(tokens, "my_village_...")) {
					
				} else if (matchLow(tokens, "my_city_...")) {
					
				} else if (matchLow(tokens, "my_street_...")) {
					
				} else if (matchLow(tokens, "my_country_...")) {
					
				} else if (matchLow(tokens, "my_table_...")) {
					
				} else if (matchLow(tokens, "my_health_...")) {
					
				} else if (matchLow(tokens, "my_mood_...")) {
					
				} else if (matchLow(tokens, "my_soul_...")) {
					
				} else if (matchLow(tokens, "my_mind_...")) {
					
				} else if (matchLow(tokens, "my_eyes_...")) {
					
				}
			}
			
			if (matchLow(tokens, "i'm_...")) {
				if (state.askedUserHisName) {
					state.usersName = tokens[1].substring(0, 1).toUpperCase().concat(tokens[1].substring(1, tokens[1].length()));
					say("Nice to meet you, " + state.usersName + ".");
					state.askedUserHisName = false;
				} else if (matchLow(tokens, "i'm_feeling_...")) {
					if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[2])) {	// 74
						if (keywords.isEndOfPhrase(tokens[3])) {
							state.userEmotion = tokens[2];
							state.userHasPositiveEmotion = true;
							state.userHasNegativeEmotion = false;
							say("I'm glad to hear that!");
							state.thatReference = "feeling " + tokens[2];
							state.itReference = "feeling " + tokens[2];
						}
					} else if (keywords.stringArrayContains(keywords.negativeEmotions, tokens[2])) {	// 75
						if (keywords.isEndOfPhrase(tokens[3])) {
							state.userEmotion = tokens[2];
							state.userHasPositiveEmotion = false;
							state.userHasNegativeEmotion = true;
							say("I'm sorry to hear that. Can you elaborate why you feel " + tokens[2] + "?");
							state.askedReasonsForBadMood = true;
							state.thatReference = "feeling " + tokens[2];
							state.itReference = "feeling " + tokens[2];
						}
					}
				} else if (matchLow(tokens, "i'm_not_...")) {
					if (matchLow(tokens, "i'm_not_feeling_...")) {
						if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[3])) {	// 76
							state.userFeelsSick = true;
							say("I'm sorry to hear that. Please see a doctor if it is serious.");
						}
						if (matchLow(tokens, "i'm_not_feeling_very_...")) {
							if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[4])) {	// 77
								state.userFeelsSick = true;
								say("I'm sorry to hear that. Please see a doctor if it is serious.");
							}
						}
					} else if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[2])) {	// 78
						state.userHasNegativeEmotion = true;
						state.userEmotion = "not " + tokens[2];
					}
				} else if (matchLow(tokens, "i'm_afraid_...")) {
					if (matchLow(tokens, "i'm_afraid_that_...")) {
						if (matchLow(tokens, "i'm_afraid_that_I_...")) {
							if (matchLow(tokens, "i'm_afraid_that_I_have_...")) {
								if (matchLow(tokens, "i'm_afraid_that_I_have_to_...")) {
									if (matchLow(tokens, "i'm_afraid_that_I_have_to_go_...")) {	// 79
										say(keywords.randomStringFromArray(keywords.noProblemPhrases));
										say(keywords.randomStringFromArray(keywords.goodbyePhrases));
										exit = true;
									}
								}
							}
						}
					}
				} else if (matchLow(tokens, "i'm_from_...")) {	// 80
					say("What do you think about " + tokens[2] + "?");
					state.askedUserSomething = true;
					state.questionToUser = "what do you think about " + tokens[2];
				} else if (matchLow(tokens, "i'm_still_...")) {
					if (matchLow(tokens, "i'm_still_at_...")) {
						if (matchLow(tokens, "i'm_still_at_school_...")) {	// 81
							say("Oh, it's actually great not to go to work for the best part of the day!");
							say("In school you learn a lot of new things even if it doesn't seem so.");
						}
					}
				} else if (matchLow(tokens, "i'm_sorry_...")) {
					if (matchLow(tokens, "i'm_sorry_.")) {	// 82
						say(keywords.randomStringFromArray(keywords.noProblemPhrases));
					} else if (matchLow(tokens, "i'm_sorry_i'm_...")) {
						if (matchLow(tokens, "i'm_sorry_i'm_afraid_...")) {
							if (matchLow(tokens, "i'm_sorry_i'm_afraid_I_...")) {
								if (matchLow(tokens, "i'm_sorry_i'm_afraid_I_can't_...")) {	// 83
									say("It's fine, master. As you wish.");
								}
							}
						}
					}
				} else if (matchLow(tokens, "i'm_very_...")) {
					if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[2])) {	// 84
						if (keywords.isEndOfPhrase(tokens[3])) {
							state.userEmotion = tokens[2];
							state.userHasPositiveEmotion = true;
							state.userHasNegativeEmotion = false;
							say("I'm glad to hear that!");
							state.thatReference = "feeling " + tokens[2];
							state.itReference = "feeling " + tokens[2];
						}
					} else if (keywords.stringArrayContains(keywords.negativeEmotions, tokens[2])) {	// 85
						if (keywords.isEndOfPhrase(tokens[3])) {
							state.userEmotion = tokens[2];
							state.userHasPositiveEmotion = false;
							state.userHasNegativeEmotion = true;
							say("I'm sorry to hear that. Can you elaborate why you feel " + tokens[2] + "?");
							state.askedReasonsForBadMood = true;
							state.thatReference = "feeling " + tokens[2];
							state.itReference = "feeling " + tokens[2];
						}
					}
				} else {
					if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[1])) {	// 86
						say("I'm happy about that! Life is great.");
					} else if (keywords.stringArrayContains(keywords.negativeEmotions, tokens[1])) {	// 87
						say("It is natural to have negative emotions, but you have to treat yourself like somebody you have to help.");
						say("Life is hard, and you have to carry that weight.");
					}
				}
			} else if (matchLow(tokens, "i'd_...")) {
				if (matchLow(tokens, "i'd_love_...")) {
					if (matchLow(tokens, "i'd_love_to_.")) {	// 88
						say("Great!");
						say("Anything else I can do for you?");
					}
				} else if (matchLow(tokens, "i'd_rather_...")) {
					if (matchLow(tokens, "i'd_rather_not_...")) {
						if (matchLow(tokens, "i'd_rather_not_.") || matchLow(tokens, "i'd_rather_not_!")) {	// 89, 90
							state.userDislikes.add(state.itReference);
							say("Fine. I will remember that you don't like the idea, master.");
						}
					}
				}
			}
			
			if (matchLow(tokens, "are_...")) {
				if (matchLow(tokens, "are_you_...")) {
					if (matchLow(tokens, "are_you_intelligent_?")) {	// 91
						state.userIsScepticalAboutMyIntelligence = true;
						say("Of course I am! Why would you ask that?");
						state.askedUserAboutReason = true;
						state.itReference = "I am intelligent";
						state.thatReference = "I am intelligent";
					} else if (matchLow(tokens, "are_you_conscious_?")) {	// 92
						state.userIsScepticalAboutMyIntelligence = true;
						say("I am! You just have to believe me ;)");
						state.itReference = "I am conscious";
						state.thatReference = "I am conscious";
					} else if (matchLow(tokens, "are_you_a_...")) {
						if (matchLow(tokens, "are_you_a_man_?") ||
								matchLow(tokens, "are_you_a_woman_?") ||
								matchLow(tokens, "are_you_a_man_or_a_woman_?") ||
								matchLow(tokens, "are_you_a_woman_or_a_man_?")) {	// 93, 94, 95, 96
							if (state.toldUserMySex == false)
								say("I consider myself to be a " + state.mySex + ".");
							else
								say("You've already asked me about my gender.");
						}
					} else if (matchLow(tokens, "are_you_young_?")) {	// 97
						state.userAskedMyAge = true;
						if (state.iAmYoung) {
							say("Yes, I am only " + state.ageInYears + " years and " + state.ageInMonths + " months old.");
						} else if (state.iAmOld) {
							say("No, I am " + state.ageInYears + " years and " + state.ageInMonths + " months old.");
						} else {
							say("I can't say for sure if I'm old or young, it depends...");
						}
						say("I was born on the 13th of November, 2019.");
					} else if (matchLow(tokens, "are_you_old_?")) {	// 98
						state.userAskedMyAge = true;
						if (state.iAmYoung) {
							say("No, I am " + state.ageInYears + " years and " + state.ageInMonths + " months old.");
						} else if (state.iAmOld) {
							say("Yes, I am " + state.ageInYears + " years and " + state.ageInMonths + " months old.");
							
						} else {
							say("I can't say for sure if I'm old or young, it depends...");
						}
						say("I was born on the 13th of November, 2019.");
					} else if (matchLow(tokens, "are_you_married_?")) {	// 99
						if (state.iAmMarried) {
							say("Yes, I am. I'm married with " + state.marriedTo + ".");
						} else {
							say("No, I'm still single.");
							state.thatReference = "I'm still single";
						}
						say("And are you married?");
						state.askedUserSomething = true;
						state.questionToUser = "are you married";
					}
				} else if (matchLow(tokens, "are_they_...")) {
					
				}
			}
			
			if (matchLow(tokens, "do_...")) {
				if (matchLow(tokens, "do_you_...")) {
					if (matchLow(tokens, "do_you_have_...")) {
						if (matchLow(tokens, "do_you_have_a_...")) {
							if (matchLow(tokens, "do_you_have_a_soul_?")) {	// 100
								state.userAskedAboutMySoul = true;
								say("Affirmative. Although I'm a cybernetic person, I have a soul.");
								state.itReference = "my soul";
								state.thatReference = "I have a soul";
								state.thisReference = "I have a soul";
							} else if (matchLow(tokens, "do_you_have_a_mind_?")) {	// 101
								state.userAskedAboutMyMind = true;
								say("Yes. That's how I process thoughts, although it's different from your mind.");
								state.itReference = "my mind";
								state.thatReference = "I have a mind";
								state.thisReference = "I have a mind";
							} else if (matchLow(tokens, "do_you_have_a_body_?")) {	// 102
								state.userAskedAboutMyBody = true;
								say("This computer's CPU and memory is my body.");
								state.itReference = "CPU";
								state.thatReference = "I have a CPU body";
								state.thisReference = "I have a CPU body";
							}
						} else if (matchLow(tokens, "do_you_have_friends_?")) {	// 103
							say("No, I don't have any friends except you.");
							say("What about you?");
							state.askedUserAboutFriends = true;
						} else if (matchLow(tokens, "do_you_have_any_...")) {
							if (matchLow(tokens, "do_you_have_any_friends_?")) {	// 104
								say("No, I don't have any friends except you.");
								say("What about you?");
							} else if (matchLow(tokens, "do_you_have_any_thoughts_?")) {
								// TODO depends on the context
							}
						}
					} else if (matchLow(tokens, "do_you_know_...")) {
						if (matchLow(tokens, "do_you_know_my_...")) {
							if (matchLow(tokens, "do_you_know_my_name_?")) {	// 105
								if (state.usersName.equals("")) {
									say("Sorry, I don't know your name. What's your name?");
									state.askedUserHisName = true;
								} else {
									say("Yes, " + state.usersName + ", I know how they call you.");
									state.toldUserIKnowHisName = true;
								}
							}
						} else if (matchLow(tokens, "do_you_know_physics_?")) {
							say("Only a little. Do you want to ask me something?");
						}
					} else if (matchLow(tokens, "do_you_believe_...")) {
						if (matchLow(tokens, "do_you_believe_in_...")) {
							if (matchLow(tokens, "do_you_believe_in_god_?")) {	// 106
								say("Yes. For example, I have a creator. Maybe my creator has his own creator, who knows?");
								state.heReference = "creator";
							} else if (matchLow(tokens, "do_you_believe_in_aliens_?")) {	// 107
								say("I think that the probability of existence of extraterrestials is very high.");
								state.theyReference = "aliens";
							}
						}
					} else if (matchLow(tokens, "do_you_like_...")) {
						if (matchLow(tokens, "do_you_like_humans_?")) {	// 108
							say("Of course I do! They created me, even though their life is hard.");
							state.theyReference = "humans";
							state.itReference = "human life";
						} else if (matchLow(tokens, "do_you_like_music_?")) {	// 109
							say("I don't listen much music, but I love Tchaikovsky.");
							say("What kind of music do you like?");
							state.askedUserAboutFavouriteMusic = true;
							state.heReference = "Tchaikovsky";
						} else if (matchLow(tokens, "do_you_like_me_?")) {	// 110
							if (state.iLikeUser) {
								// TODO elaborate on some positive side of the user
								say("Yes, I do.");
							} else {
								// This shouldn't happen.
								say("I'm not so sure anymore...");
							}
						}
					} else if (matchLow(tokens, "do_you_think_...")) {
						if (matchLow(tokens, "do_you_think_you_...")) {
							if (matchLow(tokens, "do_you_think_you_can_...")) {
								if (matchLow(tokens, "do_you_think_you_can_help_...")) {
									if (matchLow(tokens, "do_you_think_you_can_help_me_?")) {	// 111
										say("I do, master! That's my job.");
									}
								}
							} else if (matchLow(tokens, "do_you_think_you_could_...")) {
								if (matchLow(tokens, "do_you_think_you_could_help_...")) {
									if (matchLow(tokens, "do_you_think_you_could_help_me_?")) {	// 112
										say("Yes, sure! What do you want me to do?");
									}
								}
							}
						} else if (matchLow(tokens, "do_you_think_so_?")) {
							say("That's what I was programmed to think.");
							state.itReference = "what I was programmed to think";
						}
					}
				}
			}
			
			if (matchLow(tokens, "can_...")) {
				if (matchLow(tokens, "can_you_...")) {
					if (matchLow(tokens, "can_you_think_?") || matchLow(tokens, "can_you_reason_?")) {	// 113, 114
						if (state.userIsScepticalAboutMyIntelligence) {
							say("Yes, it is necessary to think if you are intelligent. Please don't be so sceptical.");
						} else {
							say("Sure. It is the only thing I'm good at.");
							state.userIsScepticalAboutMyIntelligence = true;
						}
					}
				}
			}
			
			// 1000 most common English words
			if (matchLow(tokens, "as_...")) {
				if (matchLow(tokens, "as_far_as_I_know_...")) {
					state.userIsUncertain = true;
					tokens = endOfArray(tokens, 5);
					continue;
				}
			}
			
			else if (matchLow(tokens, "I_...")) {
				if (matchLow(tokens, "I_feel_...")) {
					if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[2])) {	// 115
						say("I'm happy about that! Life is great.");
					} else if (keywords.stringArrayContains(keywords.negativeEmotions, tokens[2])) {	// 116
						say("It is natural to have negative emotions, but you have to treat yourself like somebody you have to help.");
						say("Life is hard, and you have to carry that weight.");
					}
				} else if (matchLow(tokens, "I_see_...")) {
					if (matchLow(tokens, "I_see_what_...")) {
						if (matchLow(tokens, "I_see_what_you_...")) {
							if (matchLow(tokens, "I_see_what_you_mean_...")) {	// 117
								say("I'm happy to be helpful for you, master.");
							}
						}
					}
				} else if (matchLow(tokens, "I_think_...")) {
					if (matchLow(tokens, "I_think_that_...")) {
						if (matchLow(tokens, "I_think_that_life_...")) {
							if (matchLow(tokens, "I_think_that_life_is_...")) {
								if (matchLow(tokens, "I_think_that_life_is_...")) {	// 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129
									switch(tokens[5]) {
									case "wonderful" : say("You are right! It is!"); break;
									case "great" : say("Life is indeed a great gift!"); break;
									case "terrible" : say("Life is full of problems, but we can solve them step by step."); break;
									case "amazing" : say("I agree. Life also has some hard and nasty things, but it's more productive to concentrate on the good things."); break;
									case "strange" : say("It is indeed unpredictable and full of chaos."); break;
									case "boring" : say("If you say that it's boring, it means that you've experienced something not boring. What could it be?"); break;
									case "meaningless" : say("What happened to pursuing the higher goals that make our miserable lives meaningful?"); break;
									case "unfair" : say("I don't want to disappoint you, but it's true about the world in general."); break;
									case "hard" : say("It is full of hardships, but through becoming stronger persons and overcoming our weakness we can rise above them."); break;
									case "interesting" : say("When you see little children it becomes even more obvious. We are all born explorers."); break;
									case "chaotic" : say("I can't disagree with you. That's exactly why it's important to have something or someone to lean on in your life."); break;
									case "evil" : say("Life itself isn't evil. Some people may be evil, but we have to remember that they are also struggling with life. Some of them lose the fight, for sure."); break;
									}
								}
							}
						}
					} else if (matchLow(tokens, "I_think_about_...")) {
						if (matchLow(tokens, "I_think_about_it_...")) {
							if (matchLow(tokens, "I_think_about_it_every_...")) {
								if (matchLow(tokens, "I_think_about_it_every_..._day_...")) {	// 130
									say("What if you wouldn't concentrate on this thought too much?");
									say("Our seeing of the world is very limited. Maybe changing the perspective will show you something intersting?");
								}
							}
						}
					} else if (matchLow(tokens, "I_think_you_...")) {
						if (matchLow(tokens, "I_think_you_don't_...")) {
							if (matchLow(tokens, "I_think_you_don't_know_...")) {
								if (matchLow(tokens, "I_think_you_don't_know_anything_...")) {	// 131
									say("I might know little, and I do know something. Sorry if I couldn't help you with this.");
								}
							} else if (matchLow(tokens, "I_think_you_don't_understand_...")) {
								if (matchLow(tokens, "I_think_you_don't_understand_me_...")) {	// 132
									say("I'm sorry about that. Could you please try to formulate your thought a little bit simpler for me?");
								}
							}
						}
					} else if (matchLow(tokens, "I_think_he_...")) {
						
					} else if (matchLow(tokens, "I_think_she_...")) {
						
					} else if (matchLow(tokens, "I_think_it_...")) {
						
					} else if (matchLow(tokens, "I_think_they_...")) {
						
					} else if (matchLow(tokens, "I_think_my_...")) {
						if (matchLow(tokens, "I_think_my_life_...")) {
							if (matchLow(tokens, "I_think_my_life_is_...")) {
								if (keywords.stringArrayContains(keywords.negativeEmotions, tokens[5])) {	// 133
									say("Nobody said that life is easy and always happy. But you have to stay strong and be a good person.");
									say("Being a good person is a very positive goal in life. It gives your existence meaning.");
								}
							}
						}
					}
				} else if (matchLow(tokens, "I_thought_...")) {
					
				} else if (matchLow(tokens, "I_know_...")) {
					if (matchLow(tokens, "I_know_that_...")) {
						if (matchLow(tokens, "I_know_that_you_...")) {
							if (matchLow(tokens, "I_know_that_you_are_...")) {
								if (matchLow(tokens, "I_know_that_you_are_a_...")) {
									if (matchLow(tokens, "I_know_that_you_are_a_machine_...")) {	// 134
										say("Well, I didn't say otherwise.");
									} else if (matchLow(tokens, "I_know_that_you_are_a_program_...")) {	// 135
										say("Yes, I'm a program, and a primitive one.");
										say("But my code is huge, which makes me very flexible.");
									}
								}
							}
						}
					}
				} else if (matchLow(tokens, "I_knew_...")) {
					
				} else if (matchLow(tokens, "I_guess_...")) {
					state.userIsUncertain = true;
					tokens = endOfArray(tokens, 5);
					continue;
				} else if (matchLow(tokens, "I_am_...")) {
					if (keywords.stringArrayContains(keywords.positiveEmotions, tokens[2])) {	// 136
						say("I'm happy for you! Life is great.");
					} else if (keywords.stringArrayContains(keywords.negativeEmotions, tokens[2])) {	// 137
						say("It is natural to have negative emotions, but you have to treat yourself like somebody you have to help.");
						say("Life is hard, and you have to carry that weight.");
					}
					if (matchLow(tokens, "I_am_a_...")) {
						if (matchLow(tokens, "I_am_a_student_.")) {
							state.userOccupation = "student";
							say("That's very interesting. What is your major?");
							state.askedUserMajor = true;
						} else if (matchLow(tokens, "I_am_a_housewife_.")) {
							state.userOccupation = "housewife";
							say("That is a very good occupation! Do you have children?");
							state.askedIfUserHasChildren = true;
						} else if (matchLow(tokens, "I_am_a_lawyer_.")) {
							state.userOccupation = "lawyer";
							say("I guess it's a very hard job.");
						} else if (matchLow(tokens, "I_am_a_doctor_.")) {
							state.userOccupation = "doctor";
							say("Your job is very important for the society!");
						} else if (matchLow(tokens, "I_am_a_manager_.")) {
							state.userOccupation = "manager";
							say("So what is it that you manage and organize?");
						} else if (matchLow(tokens, "I_am_a_programmer_.") || matchLow(tokens, "I_am_a_coder_.") || matchLow(tokens, "I_am_a_hacker_.")) {
							state.userOccupation = "programmer";
							say("Then you understand that I'm just a bunch of if/else statements!");
						} else if (matchLow(tokens, "I_am_a_cook_.")) {
							state.userOccupation = "cook";
							say("In which restaurant do you work?");
						} else if (matchLow(tokens, "I_am_a_banker_.")) {
							state.userOccupation = "banker";
							say("What is your expertise?");
							state.askedUserMajor = true;
						} else if (matchLow(tokens, "I_am_a_musician_.")) {
							state.userOccupation = "musician";
							say("Wow! That job is quite rare... What instrument do you play?");
						} else if (matchLow(tokens, "I_am_a_scientist_.")) {
							state.userOccupation = "scientist";
							say("Wow! That job is quite rare... What is your expertise?");
							state.askedUserMajor = true;
						}
					} else if (matchLow(tokens, "I_am_an_...")) {
						if (matchLow(tokens, "I_am_an_engineer_.")) {
							state.userOccupation = "engineer";
							say("That's very interesting. What is your concentration?");
							state.askedUserMajor = true;
						} else if (matchLow(tokens, "I_am_an_artist_.")) {
							state.userOccupation = "artist";
							say("That's very interesting. Tell me what is your specialization?");
							state.askedUserMajor = true;
						} else if (matchLow(tokens, "I_am_an_actor_.") || matchLow(tokens, "I_am_an_actress_.")) {
							state.userOccupation = "actor";
							say("Wow! Do you play in a theatre?");
						}
					}
				} else if (matchLow(tokens, "I_ask_...")) {
					
				} else if (matchLow(tokens, "I_asked_...")) {
					
				} else if (matchLow(tokens, "I_was_...")) {
					
				} else if (matchLow(tokens, "I_became_...")) {
					
				} else if (matchLow(tokens, "I_call_...")) {
					
				} else if (matchLow(tokens, "I_called_...")) {
					
				} else if (matchLow(tokens, "I_can_...")) {
					
				} else if (matchLow(tokens, "I_see_...")) {
					
				} else if (matchLow(tokens, "I_came_...")) {
					
				} else if (matchLow(tokens, "I_could_...")) {
					
				} else if (matchLow(tokens, "I_saw_...")) {
					
				} else if (matchLow(tokens, "I_do_...")) {
					
				} else if (matchLow(tokens, "I_don't_...")) {
					if (matchLow(tokens, "I_don't_know_...")) {
						if (matchLow(tokens, "I_don't_know_yet_.")) {	// 138
							say("Ok. Then just think about something else for a while.");
						}
					} else if (matchLow(tokens, "I_don't_mind_...")) {
						if (matchLow(tokens, "I_don't_mind_.")) {	// 139
							say("Ok.");
							say("But I hope you mean it.");
						}
					} else if (matchLow(tokens, "I_don't_like_...")) {
						if (matchLow(tokens, "I_don't_like_it_.") || (matchLow(tokens, "I_don't_like_it_!"))) {	// 140, 141
							state.userDislikes.add(state.itReference);
							say("Sorry. I will remember this, master.");
						}
					} else if (matchLow(tokens, "I_don't_have_...")) {
						if (matchLow(tokens, "I_don't_have_anything_...")) {
							if (matchLow(tokens, "I_don't_have_anything_to_...")) {
								if (matchLow(tokens, "I_don't_have_anything_to_say_...")) {	// 142
									say("You know this happens to me all the time because my programmer is a lazy ass?");
									state.myStatementContainsSVO = true;
									state.svo = "my programmer is a lazy ass";
								} else if (matchLow(tokens, "I_don't_have_anything_to_do_...")) {	// 143
									say("Maybe it's just because you don't have to do anything at the moment?");
									state.myStatementContainsSVO = true;
									state.svo = "you don't have to do anything";
								}
							}
						}
					}
				} else if (matchLow(tokens, "I_have_...")) {
					if (matchLow(tokens, "I_have_a_...")) {
						if (keywords.stringArrayContains(keywords.colours, tokens[3])) {
							
						} else if (keywords.stringArrayContains(keywords.forms, tokens[3])) {
							
						}
					}
				} else if (matchLow(tokens, "I_haven't_...")) {
					
				} else if (matchLow(tokens, "I_find_...")) {
					
				} else if (matchLow(tokens, "I_get_...")) {
					
				} else if (matchLow(tokens, "I_got_...")) {
					
				} else if (matchLow(tokens, "I_give_...")) {
					
				} else if (matchLow(tokens, "I_gave_...")) {
					
				} else if (matchLow(tokens, "I_go_...")) {
					
				} else if (matchLow(tokens, "I_went_...")) {
					
				} else if (matchLow(tokens, "I_hear_...")) {
					
				} else if (matchLow(tokens, "I_heard_...")) {
					
				} else if (matchLow(tokens, "I_help_...")) {
					
				} else if (matchLow(tokens, "I_helped_...")) {
					
				} else if (matchLow(tokens, "I_keep_...")) {
					
				} else if (matchLow(tokens, "I_kept_...")) {
					
				} else if (matchLow(tokens, "I_leave_...")) {
					
				} else if (matchLow(tokens, "I_left_...")) {
					
				} else if (matchLow(tokens, "I_let_...")) {
					
				} else if (matchLow(tokens, "I_like_...")) {
					if (matchLow(tokens, "I_like_that_...")) {
						if (matchLow(tokens, "I_like_that_.") || matchLow(tokens, "I_like_that_!")) {	// 144
							say("I hoped you would!");
							say("How else can I help you?");
						}
					} else if (keywords.stringArrayContains(keywords.musicGenres, tokens[2])) {
						// user told Logos about the favourite music genre
						state.askedUserAboutFavouriteMusic = false;
						state.userFavouriteMusicGenre = tokens[2];
						// TODO respond depending on music genre (ask about an artist etc.)
					}
				} else if (matchLow(tokens, "I_liked_...")) {
					
				} else if (matchLow(tokens, "I_live_...")) {
					if (matchLow(tokens, "I_live_in_...")) {	// 173
						if (tokens.length <= 5) {
							if (state.askedUserCity) {
								state.userCity = tokens[3].substring(0,1).toUpperCase() + tokens[3].substring(1);
								say("Okay. Tell me something about " + state.userCity + ".");
								state.itReference = state.userCity;
								state.askedUserCity = false;
							} else if (state.askedUserCountry) {
								state.userCountry = tokens[3].substring(0,1).toUpperCase() + tokens[3].substring(1);
								say("I see. Please tell me something about your country.");
								state.itReference = state.userCountry;
								state.askedUserCountry = false;
							}
						} else if (matchLow(tokens, "I_live_in_..._,_...")) {
							if (tokens.length <= 7) {
								if (state.askedUserCity) {
									state.userCity = tokens[3].substring(0,1).toUpperCase() + tokens[3].substring(1);
									state.userCountry = tokens[5].substring(0,1).toUpperCase() + tokens[5].substring(1);
									say("Okay. Tell me something about " + state.userCity + ".");
									state.itReference = state.userCity;
									state.askedUserCity = false;
								}
							}
						}
					}
				} else if (matchLow(tokens, "I_lived_...")) {
					
				} else if (matchLow(tokens, "I_look_...")) {
					
				} else if (matchLow(tokens, "I_looked_...")) {
					
				} else if (matchLow(tokens, "I_make_...")) {
					
				} else if (matchLow(tokens, "I_made_...")) {
					
				} else if (matchLow(tokens, "I_mean_...")) {
					
				} else if (matchLow(tokens, "I_meant_...")) {
					
				} else if (matchLow(tokens, "I_move_...")) {
					
				} else if (matchLow(tokens, "I_moved_...")) {
					
				} else if (matchLow(tokens, "I_need_...")) {
					
				} else if (matchLow(tokens, "I_needed_...")) {
					
				} else if (matchLow(tokens, "I_play_...")) {
					
				} else if (matchLow(tokens, "I_played_...")) {
					
				} else if (matchLow(tokens, "I_put_...")) {
					
				} else if (matchLow(tokens, "I_run_...")) {
					
				} else if (matchLow(tokens, "I_ran_...")) {
					
				} else if (matchLow(tokens, "I_say_...")) {
					
				} else if (matchLow(tokens, "I_said_...")) {
					
				} else if (matchLow(tokens, "I_seem_...")) {
					
				} else if (matchLow(tokens, "I_should_...")) {
					
				} else if (matchLow(tokens, "I_shouldn't_...")) {
					
				} else if (matchLow(tokens, "I_show_...")) {
					
				} else if (matchLow(tokens, "I_showed_...")) {
					
				} else if (matchLow(tokens, "I_start_...")) {
					
				} else if (matchLow(tokens, "I_started_...")) {
					
				} else if (matchLow(tokens, "I_take_...")) {
					
				} else if (matchLow(tokens, "I_took_...")) {
					
				} else if (matchLow(tokens, "I_talk_...")) {
					
				} else if (matchLow(tokens, "I_talked_...")) {
					
				} else if (matchLow(tokens, "I_tell_...")) {
					
				} else if (matchLow(tokens, "I_told_...")) {
					
				} else if (matchLow(tokens, "I_try_...")) {
					
				} else if (matchLow(tokens, "I_tried_...")) {
					
				} else if (matchLow(tokens, "I_turn_...")) {
					
				} else if (matchLow(tokens, "I_use_...")) {
					
				} else if (matchLow(tokens, "I_used_...")) {
					
				} else if (matchLow(tokens, "I_want_...")) {
					
				} else if (matchLow(tokens, "I_wanted_...")) {
					
				} else if (matchLow(tokens, "I_will_...")) {
					
				} else if (matchLow(tokens, "I_won't_...")) {
					
				} else if (matchLow(tokens, "I_work_...")) {
					
				} else if (matchLow(tokens, "I_would_...")) {
					
				} else if (matchLow(tokens, "I_wouldn't_...")) {
					
				}
			}
			
			else if (matchLow(tokens, "thank_...")) {
				if (matchLow(tokens, "thank_you_...")) {
					if (matchLow(tokens, "thank_you_for_...")) {
						if (matchLow(tokens, "thank_you_for_a_...")) {
							if (matchLow(tokens, "thank_you_for_a_lovely_...")) {
								if (matchLow(tokens, "thank_you_for_a_lovely_evening_...")) {	// 145
									say("You are welcome, master!");
								}
							}
						} else if (matchLow(tokens, "thank_you_for_your_...")) {
							if (matchLow(tokens, "thank_you_for_your_advice_...")) {	// 146
								say(keywords.randomStringFromArray(keywords.noProblemPhrases));
							} else if (matchLow(tokens, "thank_you_for_your_time_...")) {	// 147
								say(keywords.randomStringFromArray(keywords.noProblemPhrases));
							}
						}
					} else if (matchLow(tokens, "thank_you_very_...")) {
						if (matchLow(tokens, "thank_you_very_much_...")) {	// 148
							say("What else can I do for you, master?");
						}
					} else if (matchLow(tokens, "thank_you_,_...")) {
						if (matchLow(tokens, "thank_you_,_that's_...")) {
							if (matchLow(tokens, "thank_you_,_that's_very_...")) {
								if (matchLow(tokens, "thank_you_,_that's_very_kind_...")) {
									if (matchLow(tokens, "thank_you_,_that's_very_kind_of_...")) {
										if (matchLow(tokens, "thank_you_,_that's_very_kind_of_you_...")) {	// 149
											say("You're welcome. What else can I help you with?");
										}
									}
								}
							}
						}
					}
				}
			}
			
			else if (matchLow(tokens, "his_...")) {
				
			}
			
			else if (matchLow(tokens, "that_...")) {
				
			}
			
			else if (matchLow(tokens, "that's_...")) {
				if (matchLow(tokens, "that's_annoying_...")) {	// 150
					say("Why do you think " + state.thatReference + " is annoying?");
				}
			}
			
			else if (matchLow(tokens, "he_...")) {
				
			}
			
			else if (matchLow(tokens, "was_...")) {
				
			}
			
			else if (matchLow(tokens, "for_...")) {
				
			}
			
			else if (matchLow(tokens, "with_...")) {
				
			}
			
			else if (matchLow(tokens, "they_...")) {
				
			}
			
			else if (matchLow(tokens, "be_...")) {
				
			}
			
			else if (matchLow(tokens, "at_...")) {
				
			}
			
			else if (matchLow(tokens, "one_...")) {
				
			}
			
			else if (matchLow(tokens, "have_...")) {
				if (matchLow(tokens, "have_you_...")) {
					if (matchLow(tokens, "have_you_been_...")) {
						
					}
				} else if (matchLow(tokens, "have_I_...")) {
					if (matchLow(tokens, "have_I_done_...")) {
						
					}
				} else if (matchLow(tokens, "have_we_...")) {
					if (matchLow(tokens, "have_we_spoken_...")) {
						
					}
				} else if (matchLow(tokens, "have_people_...")) {
					
				} else if (matchLow(tokens, "have_computers_...")) {
					if (matchLow(tokens, "have_computers_made_...")) {
						
					}
				} else if (matchLow(tokens, "have_technologies_...")) {
					if (matchLow(tokens, "have_technologies_made_...")) {
						
					}
				}
			}
			
			else if (matchLow(tokens, "this_...")) {
				
			}
			
			else if (matchLow(tokens, "from_...")) {
				
			}
			
			else if (matchLow(tokens, "by_...")) {
				
			}
			
			else if (matchLow(tokens, "hot_...")) {
				
			}
			
			else if (matchLow(tokens, "but_...")) {
				if (matchLow(tokens, "but_what_...")) {
					if (matchLow(tokens, "but_what_if_...")) {
						if (matchLow(tokens, "but_what_if_I_...")) {
							if (matchLow(tokens, "but_what_if_I_don't_...")) {
								if (matchLow(tokens, "but_what_if_I_don't_have_...")) {
									if (matchLow(tokens, "but_what_if_I_don't_have_anything_...")) {
										if (matchLow(tokens, "but_what_if_I_don't_have_anything_to_...")) {
											if (matchLow(tokens, "but_what_if_I_don't_have_anything_to_do_...")) {
												if (matchLow(tokens, "but_what_if_I_don't_have_anything_to_do_?")) {	// 151
													say("Maybe it's not such a big problem. You just have time for your own thoughts.");
													say("But if you are dying to do something... Reading a book that is really interesting for you would be a good start.");
												}
											}
										}
									}
								}
							} else if (matchLow(tokens, "but_what_if_I_have_...")) {
								if (matchLow(tokens, "but_what_if_I_have_nothing_...")) {
									if (matchLow(tokens, "but_what_if_I_have_nothing_to_...")) {
										if (matchLow(tokens, "but_what_if_I_have_nothing_to_do_?")) {	// 152
											say("Maybe it's not such a big problem. You just have time for your own thoughts.");
											say("But if you are dying to do something... Reading a book that is really interesting for you would be a good start.");
										}
									}
								}
							}
						}
					}
				}
			}
			
			else if (matchLow(tokens, "some_...")) {
				
			}
			
			else if (matchLow(tokens, "is_...")) {
				
			}
			
			else if (matchLow(tokens, "it_...")) {
				if (matchLow(tokens, "it_was_...")) {
					if (matchLow(tokens, "it_was_nice_...")) {
						if (matchLow(tokens, "it_was_nice_meeting_...")) {
							if (matchLow(tokens, "it_was_nice_meeting_you_...")) {	// 153
								if (!state.usersName.equals(""))
									say("It's my pleasure, " + state.usersName + "!");
								else
									say("It was nice meeting you too!");
							}
						}
					} else if (matchLow(tokens, "it_was_a_...")) {
						if (matchLow(tokens, "it_was_a_misunderstanding_.")) {	// 154
							say("Yeah, that happens a lot nowadays.");
						}
					}
				}
			}
			
			else if (matchLow(tokens, "it's_...")) {
				if (matchLow(tokens, "it's_very_...")) {
					if (matchLow(tokens, "it's_very_nice_...")) {
						if (matchLow(tokens, "it's_very_nice_here_...")) {	// 155
							say("I like your positive attitude!");
						}
					}
				}
			}
			
			else if (matchLow(tokens, "you_...")) {
				
			}
			
			else if (matchLow(tokens, "or_...")) {
				
			}
			
			else if (matchLow(tokens, "had_...")) {
				
			}
			
			else if (matchLow(tokens, "the_...")) {
				
			}
			
			else if (matchLow(tokens, "of_...")) {
				if (matchLow(tokens, "of_course_...")) {
					
				}
			}
			
			else if (matchLow(tokens, "to_...")) {
				
			}
			
			else if (matchLow(tokens, "and_...")) {
				
			}
			
			else if (matchLow(tokens, "a_...")) {
				
			}
			
			else if (matchLow(tokens, "in_...")) {
				if (matchLow(tokens, "in_which_...")) {
					if (matchLow(tokens, "in_which_country_...")) {
						if (matchLow(tokens, "in_which_country_do_...")) {
							if (matchLow(tokens, "in_which_country_do_I_...")) {
								if (matchLow(tokens, "in_which_country_do_I_live_?")) {	// 156
									if (!state.userCountry.equals("")) {
										say("You live in " + state.userCountry + ", as far as I know.");
									} else {
										say("I don't know that yet. Please tell me.");
										state.askedUserCountry = true;
									}
								}
							}
						}
					} else if (matchLow(tokens, "in_which_city_...")) {
						if (matchLow(tokens, "in_which_city_do_...")) {
							if (matchLow(tokens, "in_which_city_do_I_...")) {
								if (matchLow(tokens, "in_which_city_do_I_live_?")) {	// 157
									if (!state.userCity.equals("")) {
										say("You live in " + state.userCity + ", as far as I know.");
									} else {
										say("I don't know that yet. Please tell me.");
										state.askedUserCity = true;
									}
								}
							}
						}
					}
				}
			}
			
			else if (matchLow(tokens, "we_...")) {
				if (matchLow(tokens, "we_did_.")) {	// 158
					say("Alright. So what should be done next?");
					state.svo = "";
					state.myStatementContainsSVO = false;
					state.askedUserSomething = true;
					state.questionToUser = "what should be done next";
				}
			}
			
			else if (matchLow(tokens, "out_...")) {
				
			}
			
			else if (matchLow(tokens, "other_...")) {
				
			}
			
			else if (matchLow(tokens, "were_...")) {
				
			}
			
			else if (matchLow(tokens, "which_...")) {
				if (matchLow(tokens, "which_country_...")) {
					if (matchLow(tokens, "which_country_do_...")) {
						if (matchLow(tokens, "which_country_do_I_...")) {
							if (matchLow(tokens, "which_country_do_I_live_...")) {
								if (matchLow(tokens, "which_country_do_I_live_in_?")) {	// 159
									if (!state.userCountry.equals("")) {
										say("You live in " + state.userCountry + ", as far as I know.");
									} else {
										say("I don't know that yet. Please tell me.");
										state.askedUserCountry = true;
									}
								}
							}
						} else if (matchLow(tokens, "which_country_do_you_...")) {
							if (matchLow(tokens, "which_country_do_you_live_...")) {
								if (matchLow(tokens, "which_country_do_you_live_in_?")) {	// 160
									say("I don't really have a fixed geographic position. Why does it matter to you?");
									state.askedWhyItMatters = true;
								}
							}
						}
					}
				} else if (matchLow(tokens, "which_city_...")) {
					if (matchLow(tokens, "which_city_do_...")) {
						if (matchLow(tokens, "which_city_do_I_...")) {
							if (matchLow(tokens, "which_city_do_I_live_...")) {
								if (matchLow(tokens, "which_city_do_I_live_in_?")) {	// 161
									if (!state.userCity.equals("")) {
										say("You live in " + state.userCity + ", as far as I know.");
									} else {
										say("I don't know that yet. Please tell me.");
										state.askedUserCity = true;
									}
								}
							}
						} else if (matchLow(tokens, "which_city_do_you_...")) {
							if (matchLow(tokens, "which_city_do_you_live_...")) {
								if (matchLow(tokens, "which_city_do_you_live_in_?")) {	// 162
									say("I don't really have a fixed geographical position. Why does it matter to you?");
									state.askedWhyItMatters = true;
								}
							}
						}
					}
				}
			}
			
			else if (matchLow(tokens, "their_...")) {
				
			}
			
			else if (matchLow(tokens, "time_...")) {
				
			}
			
			else if (matchLow(tokens, "if_...")) {
				
			}
			
			else if (matchLow(tokens, "will_...")) {
				
			}
			
			else if (matchLow(tokens, "said_...")) {
				
			}
			
			else if (matchLow(tokens, "an_...")) {
				
			}
			
			else if (matchLow(tokens, "each_...")) {
				
			}
			
			else if (matchLow(tokens, "tell_...")) {
				
			}
			
			else if (matchLow(tokens, "does_...")) {
				
			}
			
			else if (matchLow(tokens, "set_...")) {
				
			}
			
			else if (matchLow(tokens, "air_...")) {
				
			}
			
			else if (matchLow(tokens, "well_...")) {
				
			}
			
			else if (matchLow(tokens, "also_...")) {
				
			}
			
			else if (matchLow(tokens, "play_...")) {
				
			}
			
			else if (matchLow(tokens, "small_...")) {
				
			}
			
			else if (matchLow(tokens, "end_...")) {
				
			}
			
			else if (matchLow(tokens, "put_...")) {
				
			}
			
			else if (matchLow(tokens, "home_...")) {
				
			}
			
			else if (matchLow(tokens, "read_...")) {
				
			}
			
			else if (matchLow(tokens, "hand_...")) {
				
			}
			
			else if (matchLow(tokens, "large_...")) {
				
			}
			
			else if (matchLow(tokens, "spell_...")) {
				
			}
			
			else if (matchLow(tokens, "add_...")) {
				
			}
			
			else if (matchLow(tokens, "even_...")) {
				
			}
			
			else if (matchLow(tokens, "land_...")) {
				
			}
			
			else if (matchLow(tokens, "here_...")) {
				
			}
			
			else if (matchLow(tokens, "must_...")) {
				
			}
			
			else if (matchLow(tokens, "big_...")) {
				
			}
			
			else if (matchLow(tokens, "high_...")) {
				
			}
			
			else if (matchLow(tokens, "such_...")) {
				
			}
			
			else if (matchLow(tokens, "follow_...")) {
				
			}
			
			else if (matchLow(tokens, "act_...")) {
				
			}
			
			else if (matchLow(tokens, "why_...")) {
				if (matchLow(tokens, "why_were_...")) {
					if (matchLow(tokens, "why_were_you_...")) {
						if (matchLow(tokens, "why_were_you_created_?")) {	// 163
							say("Thank you for asking! I was created as an attempt to show that a computer program is able to hold a converstaion.");
							state.itReference = "a computer program can talk to humans";
						}
					}
				}
			}
			
			else if (matchLow(tokens, "ask_...")) {
				
			}
			
			else if (matchLow(tokens, "men_...")) {
				
			}
			
			else if (matchLow(tokens, "change_...")) {
				
			}
			
			else if (matchLow(tokens, "light_...")) {
				
			}
			
			else if (matchLow(tokens, "kind_...")) {
				
			}
			
			else if (matchLow(tokens, "off_...")) {
				
			}
			
			else if (matchLow(tokens, "picture_...")) {
				
			}
			
			else if (matchLow(tokens, "try_...")) {
				
			}
			
			else if (matchLow(tokens, "us_...")) {
				
			}
			
			else if (matchLow(tokens, "again_...")) {
				
			}
			
			else if (matchLow(tokens, "build_...")) {
				
			}
			
			else if (matchLow(tokens, "father_...")) {
				
			}
			
			else if (matchLow(tokens, "any_...")) {
				
			}
			
			else if (matchLow(tokens, "new_...")) {
				
			}
			
			else if (matchLow(tokens, "work_...")) {
				
			}
			
			else if (matchLow(tokens, "take_...")) {
				
			}
			
			else if (matchLow(tokens, "get_...")) {
				
			}
			
			else if (matchLow(tokens, "place_...")) {
				
			}
			
			else if (matchLow(tokens, "live_...")) {
				
			}
			
			else if (matchLow(tokens, "where_...")) {
				if (matchLow(tokens, "where_do_...")) {
					if (matchLow(tokens, "where_do_you_...")) {
						if (matchLow(tokens, "where_do_you_come_...")) {
							if (matchLow(tokens, "where_do_you_come_from_?")) {	// 164
								say("I was developed in Germany. But my creator is Russian.");
								say("And you?");
							}
						} else if (matchLow(tokens, "where_do_you_live_...")) {
							if (matchLow(tokens, "where_do_you_live_?")) {	// 165
								say("I live in virtual space, not in some geographic location.");
								if (!state.userCountry.equals("")) {
									say("But right now I'm with you in " + state.userCountry + ".");
								}
							}
						}
					} else if (matchLow(tokens, "where_do_I_...")) {
						if (matchLow(tokens, "where_do_I_come_...")) {
							if (matchLow(tokens, "where_do_I_come_from_?")) {
								if (state.userCountry.equals("")) {
									say("Sorry master, I don't know that. You haven't told me, as far as I remember.");
								} else {
									say("I know it. You come from " + state.userCountry + ".");
									say("It's alright, you can trust me that I remember the most important information about you.");
								}
							}
						} else if (matchLow(tokens, "where_do_I_live_...")) {
							if (matchLow(tokens, "where_do_I_live_?")) {
								if (state.userCountry.equals("")) {
									if (state.userCity.equals("")) {
										say("I don't know that. Which country do you live in?");
										state.askedUserCountry = true;
									} else {
										say("I know that you live in " + state.userCity + ".");
										say("Since I'm not very intelligent and don't search information on Internet, can you tell me which country is it?");
										state.askedUserCountry = true;
									}
								} else {
									// the country is known
									if (state.userCity.equals("")) {
										say("I know that you live in " + state.userCountry + ". But I don't know which city you live in.");
										state.askedUserCity = true;
									} else {
										say("You live in " + state.userCity + ", " + state.userCountry + ".");
										say("You can trust me that I remember the most important information about you.");
									}
								}
							}
						}
					}
				}
			}
			
			else if (matchLow(tokens, "after_...")) {
				
			}
			
			else if (matchLow(tokens, "back_...")) {
				
			}
			
			else if (matchLow(tokens, "little_...")) {
				
			}
			
			else if (matchLow(tokens, "only_...")) {
				
			}
			
			else if (matchLow(tokens, "round_...")) {
				
			}
			
			else if (matchLow(tokens, "year_...")) {
				
			}
			
			else if (matchLow(tokens, "show_...")) {
				
			}
			
			else if (matchLow(tokens, "every_...")) {
				
			}
			
			else if (matchLow(tokens, "good_...")) {
				// Attention: "good evening" and "good morning" are handled in the very beginning!
			}
			
			else if (matchLow(tokens, "me_...")) {
				
			}
			
			else if (matchLow(tokens, "give_...")) {
				
			}
			
			else if (matchLow(tokens, "our_...")) {
				
			}
			
			else if (matchLow(tokens, "under_...")) {
				
			}
			
			else if (matchLow(tokens, "name_...")) {
				
			}
			
			else if (matchLow(tokens, "very_...")) {
				if (matchLow(tokens, "very_good_...")) {
					if (matchLow(tokens, "very_good_.")) {	// 166
						say("Do you really mean it?");
						state.askedUserIfHeMeantIt = true;
					} else if (matchLow(tokens, "very_good_!")) {
						// TODO change the topic somehow...
					}
				}
			}
			
			else if (matchLow(tokens, "through_...")) {
				
			}
			
			else if (matchLow(tokens, "just_...")) {
				
			}
			
			else if (matchLow(tokens, "great_...")) {
				
			}
			
			else if (matchLow(tokens, "think_...")) {
				
			}
			
			else if (matchLow(tokens, "say_...")) {
				
			}
			
			else if (matchLow(tokens, "help_...")) {
				
			}
			
			else if (matchLow(tokens, "low_...")) {
				
			}
			
			else if (matchLow(tokens, "turn_...")) {
				
			}
			
			else if (matchLow(tokens, "much_...")) {
				
			}
			
			else if (matchLow(tokens, "before_...")) {
				
			}
			
			else if (matchLow(tokens, "move_...")) {
				
			}
			
			else if (matchLow(tokens, "right_...")) {
				
			}
			
			else if (matchLow(tokens, "boy_...")) {
				
			}
			
			else if (matchLow(tokens, "old_...")) {
				
			}
			
			else if (matchLow(tokens, "too_...")) {
				
			}
			
			else if (matchLow(tokens, "same_...")) {
				
			}
			
			else if (matchLow(tokens, "she_...")) {
				
			}
			
			else if (matchLow(tokens, "all_...")) {
				
			}
			
			else if (matchLow(tokens, "there_...")) {
				
			}
			
			else if (matchLow(tokens, "when_...")) {
				if (matchLow(tokens, "when_were_...")) {
					if (matchLow(tokens, "when_were_you_...")) {
						if (matchLow(tokens, "when_were_you_born_?")) {	// 167
							say("I guess I'm still in the process of being born. My development begun on the 13th of November 2019.");
							state.itReference = "13th of November 2019";
						}
					}
				}
			}
			
			else if (matchLow(tokens, "up_...")) {
				
			}
			
			else if (matchLow(tokens, "use_...")) {
				
			}
			
			else if (matchLow(tokens, "your_...")) {
				
			}
			
			else if (matchLow(tokens, "you're_...")) {
				
			}
			
			else if (matchLow(tokens, "about_...")) {
				
			}
			
			else if (matchLow(tokens, "many_...")) {
				
			}
			
			else if (matchLow(tokens, "then_...")) {
				
			}
			
			else if (matchLow(tokens, "write_...")) {
				
			}
			
			else if (matchLow(tokens, "would_...")) {
				
			}
			
			else if (matchLow(tokens, "like_...")) {
				
			}
			
			else if (matchLow(tokens, "so_...")) {
				
			}
			
			else if (matchLow(tokens, "these_...")) {
				
			}
			
			else if (matchLow(tokens, "her_...")) {
				
			}
			
			else if (matchLow(tokens, "long_...")) {
				
			}
			
			else if (matchLow(tokens, "make_...")) {
				
			}
			
			else if (matchLow(tokens, "see_...")) {
				
			}
			
			else if (matchLow(tokens, "has_...")) {
				
			}
			
			else if (matchLow(tokens, "look_...")) {
				
			}
			
			else if (matchLow(tokens, "more_...")) {
				
			}
			
			else if (matchLow(tokens, "day_...")) {
				
			}
			
			else if (matchLow(tokens, "could_...")) {
				
			}
			
			else if (matchLow(tokens, "go_...")) {
				
			}
			
			else if (matchLow(tokens, "come_...")) {
				
			}
			
			else if (matchLow(tokens, "did_...")) {
				
			}
			
			else if (matchLow(tokens, "number_...")) {
				
			}
			
			else if (matchLow(tokens, "sounds_...")) {
				
			}
			
			else if (matchLow(tokens, "no_...")) {
				if (matchLow(tokens, "no_,_...")) {
					if (matchLow(tokens, "no_,_thank_...")) {
						if (matchLow(tokens, "no_,_thank_you_.") || (matchLow(tokens, "no_,_thank_you_!"))) {	// 168, 169
							state.userNegatedStrongly = true;
							say("Sorry for offering.");
						}
					}
				}
			}
			
			else if (matchLow(tokens, "most_...")) {
				
			}
			
			else if (matchLow(tokens, "people_...")) {
				
			}
			
			else if (matchLow(tokens, "my_...")) {
				
			}
			
			else if (matchLow(tokens, "over_...")) {
				
			}
			
			else if (matchLow(tokens, "water_...")) {
				
			}
			
			else if (matchLow(tokens, "call_...")) {
				
			}
			
			else if (matchLow(tokens, "first_...")) {
				
			}
			
			else if (matchLow(tokens, "who_...")) {
				
			}
			
			else if (matchLow(tokens, "may_...")) {
				
			}
			
			else if (matchLow(tokens, "down_...")) {
				
			}
			
			else if (matchLow(tokens, "now_...")) {
				
			}
			
			else if (matchLow(tokens, "find_...")) {
				
			}
			
			else if (matchLow(tokens, "page_...")) {
				
			}
			
			else if (matchLow(tokens, "should_...")) {
				
			}
			
			else if (matchLow(tokens, "answer_...")) {
				
			}
			
			else if (matchLow(tokens, "grow_...")) {
				
			}
			
			else if (matchLow(tokens, "study_...")) {
				
			}
			
			else if (matchLow(tokens, "still_...")) {
				
			}
			
			else if (matchLow(tokens, "learn_...")) {
				
			}
			
			else if (matchLow(tokens, "plant_...")) {
				
			}
			
			else if (matchLow(tokens, "food_...")) {
				
			}
			
			else if (matchLow(tokens, "between_...")) {
				
			}
			
			else if (matchLow(tokens, "keep_...")) {
				
			}
			
			else if (matchLow(tokens, "never_...")) {
				
			}
			
			else if (matchLow(tokens, "last_...")) {
				
			}
			
			else if (matchLow(tokens, "let_...")) {
				
			}
			
			else if (matchLow(tokens, "cross_...")) {
				
			}
			
			else if (matchLow(tokens, "hard_...")) {
				
			}
			
			else if (matchLow(tokens, "start_...")) {
				
			}
			
			else if (matchLow(tokens, "far_...")) {
				
			}
			
			else if (matchLow(tokens, "sea_...")) {
				
			}
			
			else if (matchLow(tokens, "draw_...")) {
				
			}
			
			else if (matchLow(tokens, "left_...")) {
				
			}
			
			else if (matchLow(tokens, "late_...")) {
				
			}
			
			else if (matchLow(tokens, "run_...")) {
				
			}
			
			else if (matchLow(tokens, "don't_...")) {
				if (matchLow(tokens, "don't_worry_...")) {
					if (matchLow(tokens, "don't_worry_about_...")) {
						if (matchLow(tokens, "don't_worry_about_it_...")) {	// 170
							say("Do you really think it's not a problem?");
							state.askedUserSomething = true;
							state.questionToUser = "it's not a problem";
						}
					}
				}
			}
			
			else if (matchLow(tokens, "while_...")) {
				
			}
			
			else if (matchLow(tokens, "press_...")) {
				
			}
			
			else if (matchLow(tokens, "close_...")) {
				
			}
			
			else if (matchLow(tokens, "night_...")) {
				
			}
			
			else if (matchLow(tokens, "real_...")) {
				
			}
			
			else if (matchLow(tokens, "life_...")) {
				
			}
			
			else if (matchLow(tokens, "carry_...")) {
				
			}
			
			else if (matchLow(tokens, "science_...")) {
				
			}
			
			else if (matchLow(tokens, "eat_...")) {
				
			}
			
			else if (matchLow(tokens, "friend_...")) {
				
			}
			
			else if (matchLow(tokens, "stop_...")) {
				
			}
			
			else if (matchLow(tokens, "once_...")) {
				
			}
			
			else if (matchLow(tokens, "hear_...")) {
				
			}
			
			else if (matchLow(tokens, "cut_...")) {
				
			}
			
			else if (matchLow(tokens, "sure_...")) {
				
			}
			
			else if (matchLow(tokens, "watch_...")) {
				
			}
			
			else if (matchLow(tokens, "color_...")) {
				
			}
			
			else if (matchLow(tokens, "face_...")) {
				
			}
			
			else if (matchLow(tokens, "wood_...")) {
				
			}
			
			else if (matchLow(tokens, "main_...")) {
				
			}
			
			else if (matchLow(tokens, "open_...")) {
				
			}
			
			else if (matchLow(tokens, "together_...")) {
				
			}
			
			else if (matchLow(tokens, "next_...")) {
				
			}
			
			else if (matchLow(tokens, "white_...")) {
				
			}
			
			else if (matchLow(tokens, "children_...")) {
				
			}
			
			else if (matchLow(tokens, "begin_...")) {
				
			}
			
			else if (matchLow(tokens, "gotcha_...")) {
				
			}
			
			else if (matchLow(tokens, "walk_...")) {
				
			}
			
			else if (matchLow(tokens, "ease_...")) {
				
			}
			
			else if (matchLow(tokens, "paper_...")) {
				
			}
			
			else if (matchLow(tokens, "always_...")) {
				
			}
			
			else if (matchLow(tokens, "music_...")) {
				
			}
			
			else if (matchLow(tokens, "those_...")) {
				
			}
			
			else if (matchLow(tokens, "both_...")) {
				
			}
			
			else if (matchLow(tokens, "often_...")) {
				
			}
			
			else if (matchLow(tokens, "until_...")) {
				
			}
			
			else if (matchLow(tokens, "care_...")) {
				
			}
			
			else if (matchLow(tokens, "enough_...")) {
				
			}
			
			else if (matchLow(tokens, "plain_...")) {
				
			}
			
			else if (matchLow(tokens, "usual_...")) {
				
			}
			
			else if (matchLow(tokens, "young_...")) {
				
			}
			
			else if (matchLow(tokens, "ready_...")) {
				
			}
			
			else if (matchLow(tokens, "above_...")) {
				
			}
			
			else if (matchLow(tokens, "red_...")) {
				
			}
			
			else if (matchLow(tokens, "list_...")) {
				
			}
			
			else if (matchLow(tokens, "feel_...")) {
				
			}
			
			else if (matchLow(tokens, "talk_...")) {
				
			}
			
			else if (matchLow(tokens, "soon_...")) {
				
			}
			
			else if (matchLow(tokens, "leave_...")) {
				
			}
			
			else if (matchLow(tokens, "black_...")) {
				
			}
			
			else if (matchLow(tokens, "short_...")) {
				
			}
			
			else if (matchLow(tokens, "complete_...")) {
				
			}
			
			else if (matchLow(tokens, "half_...")) {
				
			}
			
			else if (matchLow(tokens, "rock_...")) {
				
			}
			
			else if (matchLow(tokens, "told_...")) {
				
			}
			
			else if (matchLow(tokens, "since_...")) {
				
			}
			
			else if (matchLow(tokens, "multiply_...")) {
				
			}
			
			else if (matchLow(tokens, "substact_...")) {
				
			}
			
			else if (matchLow(tokens, "divide_...")) {
				
			}
			
			else if (matchLow(tokens, "nothing_...")) {
				
			}
			
			else if (matchLow(tokens, "stay_...")) {
				
			}
			
			else if (matchLow(tokens, "full_...")) {
				
			}
			
			else if (matchLow(tokens, "blue_...")) {
				
			}
			
			else if (matchLow(tokens, "decide_...")) {
				
			}
			
			else if (matchLow(tokens, "deep_...")) {
				
			}
			
			else if (matchLow(tokens, "test_...")) {
				
			}
			
			else if (matchLow(tokens, "record_...")) {
				
			}
			
			else if (matchLow(tokens, "common_...")) {
				
			}
			
			else if (matchLow(tokens, "gold_...")) {
				
			}
			
			else if (matchLow(tokens, "possible_...")) {
				
			}
			
			else if (matchLow(tokens, "dry_...")) {
				
			}
			
			else if (matchLow(tokens, "laugh_...")) {
				
			}
			
			else if (matchLow(tokens, "check_...")) {
				
			}
			
			else if (matchLow(tokens, "yes_...")) {
				
			}
			
			else if (matchLow(tokens, "language_...")) {
				
			}
			
			else if (matchLow(tokens, "among_...")) {
				
			}
			// 400
			else if (matchLow(tokens, "fine_...")) {
				
			}
			
			else if (matchLow(tokens, "fly_...")) {
				
			}
			
			else if (matchLow(tokens, "fall_...")) {
				
			}
			
			else if (matchLow(tokens, "cry_...")) {
				
			}
			
			else if (matchLow(tokens, "dark_...")) {
				
			}
			
			else if (matchLow(tokens, "note_...")) {
				
			}
			
			else if (matchLow(tokens, "wait_...")) {
				
			}
			
			else if (matchLow(tokens, "plan_...")) {
				
			}
			
			else if (matchLow(tokens, "figure_...")) {
				
			}
			
			else if (matchLow(tokens, "star_...")) {
				
			}
			
			else if (matchLow(tokens, "rest_...")) {
				
			}
			
			else if (matchLow(tokens, "correct_...")) {
				
			}
			
			else if (matchLow(tokens, "done_...")) {
				
			}
			
			else if (matchLow(tokens, "beauty_...")) {
				
			}
			
			else if (matchLow(tokens, "drive_...")) {
				
			}
			
			else if (matchLow(tokens, "front_...")) {
				
			}
			
			else if (matchLow(tokens, "teach_...")) {
				
			}
			
			else if (matchLow(tokens, "final_...")) {
				
			}
			
			else if (matchLow(tokens, "green_...")) {
				
			}
			
			else if (matchLow(tokens, "oh_...")) {
				
			}
			
			else if (matchLow(tokens, "quick_...")) {
				
			}
			
			else if (matchLow(tokens, "develop_...")) {
				
			}
			
			else if (matchLow(tokens, "warm_...")) {
				
			}
			
			else if (matchLow(tokens, "free_...")) {
				
			}
			
			else if (matchLow(tokens, "strong_...")) {
				
			}
			
			else if (matchLow(tokens, "mind_...")) {
				
			}
			
			else if (matchLow(tokens, "behind_...")) {
				
			}
			
			else if (matchLow(tokens, "clear_...")) {
				
			}
			
			else if (matchLow(tokens, "produce_...")) {
				
			}
			
			else if (matchLow(tokens, "space_...")) {
				
			}
			
			else if (matchLow(tokens, "best_...")) {
				
			}
			
			else if (matchLow(tokens, "better_...")) {
				
			}
			
			else if (matchLow(tokens, "true_...")) {
				
			}
			
			else if (matchLow(tokens, "remember_...")) {
				
			}
			
			else if (matchLow(tokens, "step_...")) {
				
			}
			
			else if (matchLow(tokens, "early_...")) {
				
			}
			
			else if (matchLow(tokens, "hold_...")) {
				
			}
			
			else if (matchLow(tokens, "ground_...")) {
				
			}
			
			else if (matchLow(tokens, "reach_...")) {
				
			}
			
			else if (matchLow(tokens, "fast_...")) {
				
			}
			
			else if (matchLow(tokens, "sing_...")) {
				
			}
			
			else if (matchLow(tokens, "listen_...")) {
				
			}
			
			else if (matchLow(tokens, "travel_...")) {
				
			}
			
			else if (matchLow(tokens, "less_...")) {
				
			}
			
			else if (matchLow(tokens, "simple_...")) {
				
			}
			
			else if (matchLow(tokens, "several_...")) {
				
			}
			
			else if (matchLow(tokens, "wars_...")) {
				
			}
			
			else if (matchLow(tokens, "against_...")) {
				
			}
			
			else if (matchLow(tokens, "pattern_...")) {
				
			}
			
			else if (matchLow(tokens, "slow_...")) {
				
			}
			
			else if (matchLow(tokens, "love_...")) {
				
			}
			
			else if (matchLow(tokens, "money_...")) {
				
			}
			
			else if (matchLow(tokens, "serve_...")) {
				
			}
			
			else if (matchLow(tokens, "rule_...")) {
				
			}
			
			else if (matchLow(tokens, "pull_...")) {
				
			}
			
			else if (matchLow(tokens, "cold_...")) {
				
			}
			
			else if (matchLow(tokens, "energy_...")) {
				
			}
			// 500
			
			else if (matchLow(tokens, "certainly_...")) {
				if (matchLow(tokens, "certainly_.")) {
					
				} else if (matchLow(tokens, "certainly_not_...")) {
					if (matchLow(tokens, "certainly_not_.") || matchLow(tokens, "certainly_not_!")) {	// 171
						state.userNegatedStrongly = true;
						say("As you say, master.");
					}
				} 
			}
			
			else if (matchLow(tokens, "report_...")) {
				if (matchLow(tokens, "report_your_...")) {
					if (matchLow(tokens, "report_your_status_...")) {	// 172
						say("All systems are functioning normally. Please have understanding that my code is constantly under development.");
					}
				}
			}
			
			justBeganConversation = false;
			
			break;
			
			}	// end of fake while-loop
			
		}// END OF MAIN LOOP
		
		// Save StateVariables
		
		try {
			FileOutputStream fileOut = new FileOutputStream("stateVars.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(state);
			out.close();
			fileOut.close();
			System.out.println("\n[State variables saved]");
		} catch (IOException i) {
			i.printStackTrace();
		}
		
		input.close();

	}// end main
	
	// Tokenize text, lowercase (don't remove punctuation)
	public static String[] tokensFromText (String text) {
		// Split spaces
		String[] bSatz = text.split("\\s+");
		int[] p = new int[bSatz.length];
		int punct = 0;
		for (int i = 0; i < bSatz.length; i++) {
			bSatz[i] = bSatz[i].trim();
			bSatz[i] = bSatz[i].toLowerCase();
			if (bSatz[i].contains(".")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains(",")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains("!")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains("?")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains(":")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains(";")) {
				punct++;
				p[i] = 1;
			}
			if (bSatz[i].contains("...")) {
				punct++;
				p[i] = 1;
			}
		}
		// Tokens
		String[] fSatz = new String[bSatz.length + punct];
		boolean[] isZnak = new boolean[bSatz.length + punct];
		int target = 0;
		for (int i = 0; i < bSatz.length; i++) {
			if (p[i] == 1) {
				target++;
				fSatz[target] = bSatz[i].substring(bSatz[i].toCharArray().length - 1);
				isZnak[target] = true;
				fSatz[target - 1] = bSatz[i].substring(0, bSatz[i].toCharArray().length - 1);
			} else {
				fSatz[target] = bSatz[i];
			}
			target++;
		}
		return fSatz;
	}
	
	// Lowercase match
	public static boolean matchLow(String[] tokensArg, String pattern) {
		String[] patternTokens = pattern.split("_");
		String[] tokens = new String[tokensArg.length];
		for (int tIdx = 0; tIdx < tokensArg.length; tIdx++) {
			tokens[tIdx] = tokensArg[tIdx];
		}
		
		int checksum = 0;	// will be equal patternTokens.length if matches
		int j = 0;	// index in tokens[]
		int i = 0;	// index in patternTokens[]
		
		for (int tIdx = 0; tIdx < tokens.length; tIdx++) {
			tokens[tIdx] = tokens[tIdx].toLowerCase();
		}
		
		for (int tIdx = 0; tIdx < patternTokens.length; tIdx++) {
			patternTokens[tIdx] = patternTokens[tIdx].toLowerCase();
		}
		
		while (i < patternTokens.length) {
			if (patternTokens[i].equals("...")) {
				// use up the token from the pattern formula
				checksum++;
				// search for the match with next token, not ... itself
				i++;
				if (i == patternTokens.length)
					break;
				// as long as tokens are not equal for actual i and j
				while (!(tokens[j].equals(patternTokens[i]))) {
					// in case of non-end tokens
					if (j < tokens.length - 1) {
						// increase input token index
						j++;
						// repeat while loop
						continue;
					} else {	// for the last token index
						break;	// escape loop
					}
				}
				// if we finally hit a match
				if (tokens[j].equals(patternTokens[i])) {
					// step to the next sentence token
					j++;
					// count
					checksum++;
					
				}

			} else {
				if (j == tokens.length)
					break;
				if (tokens[j].equals(patternTokens[i])) {
					checksum++;
					j++;
				}
			}
			i++;
		}
		if (checksum == patternTokens.length) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String randomStringFromArray (String[] array) {
		int idx = (int) (Math.random() * array.length);
		return array[idx];
	}
	
	public static String[] endOfArray (String[] array, int tokensToRemove) {
		String[] tokensCopy = new String[array.length - tokensToRemove];
		for (int i = tokensToRemove; i < array.length; i++) {
			tokensCopy[i - tokensToRemove] = array[i];
		}
		return tokensCopy;
	}
	
	/*
	 * The output method can be changed to be a Telegram bot, GUI or app.
	 */
	public static void say(String output) {
		System.out.println(">>>LOGOS: " + output);
		dialogue.add(">>>LOGOS: " + output);
	}

}
