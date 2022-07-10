# WORDLINSIX
Java CLI to solve any WORDLE-style puzzles in a maximum of six tries (typically 4) when the optimum start word(s) are used

It is also possible to play the variation of wordle called _scholardle_ (currently with a 99.999% success rate, however the dictionary can change from time to time, last change was around July 2nd, 2022).

Just use **scholardle** as the first parameter (to use the recent 6-letter version of scholardle)

Alternatively use **five** as the first parameter (to play a generic game with 5-letters, such as the version of scholardle before 2nd July 2022<sup>*1</sup>) 

# Test my start word
Ever wondered how good your favourite start words are for wordle or scholardle?  
This code can indicate how likely you are to solve the game  
For example, _eileen_ is **NOT** the best start word for scholardle, evidently 216 games are destined to fail (using the algorithm in this code, which is pretty good in itself)

```scholardle debug=3 guess1=eileen```

```
#Tries	eileen
1	1
2	155
3	3286
4	6150
5	2443
6	560
7	150
8	50
9	13
10	2
11	1
12	0
13	0
14	0
15	0
===== ======= 
%PASS  99.983
FAIL   (216)
```

## N.B.
<sup>*1</sup>n.b. 
The supplied 6-letter scholardle list resource is a somewhat bizarre subset of _possible_<sup>*2</sup> words contains six letters.

<sup>*2</sup>n.b.
'Possible' in the sense that they will be accepted by the new version of scholardle, but evidently many of the words are nonesense, misspelt and/or plainly not English in any reasonable dictionary.

resources/scholardle.txt in the jar can be edited as required if it does not prove reliable. See **debug=1** below

An experimental parameter **ai=true** is under development and is showing some improvements, but should not be trusted (yet!)

# Debug Features
The code can be used to determine the letter distribution of the specified dictionary (use parameter debug=1)

The code can be used to "play itself" to determine the best starting word(s) according to its solving algorithm (use parameter debug=2)

The code can be used to check the performance of different starting words (use parameter debug=3)

# Start Word(s)
As determined by using the debug features, each variation of the game wordle / scholardle / xxxxx will have different chances of success dependent on the first word chosen. 

The suggested approach for success is to always use the right start word. For wordle use **_thump_**, for scholardle use **_biskup_**

Other generic games with different dictionaries (which, by the way, can always be added to the WORDLINSIX jar file without the need to re-compile, just use 7-zip or similar) may benefit by a second or third start word.  
The static anlysis using debug=2 will be required to find the best start word(s).

If the first word yields no matching letters (i.e., all grey on the UI) choose a second recommendation (if any)

If the second word yields no matching letters (i.e., all grey on the UI) choose a third recommendation (if any)

In the unlucky event all three starting words give no clue, do not dispair: you have eliminated half the alphabet!


# Usage
### >java -jar wordlinsix.jar (parameters)

## Parameter Help:
        Make the first parameter wordle or scholardle or five to play different variations of the game
        The columns are implicitly numbered left to right 1 through N: thus 1 is first and N is last
        Use 1=b to indicate first letter is definitely 'b'
        Eliminate letters by using not=abcdefg etc.
        Use 1=a to indicate first letter is definitely 'a'
        Use 5=z to indicate last letter is definitely 'z'
        Use 2=j 3=k 4=l to indicate letter 'j' is in column 2 and 'k' in column 3 and 'l' in 4
        Use contains=iou to indicate letters 'i' and 'o' and 'u' *must* appear in the word
        Use not2=ab to indicate second letter cannot be 'a' or 'b'
        Use not5=y to indicate last letter cannot be 'y'
        Use words=no if you don't want to see possible words
        Use rank=no if you don't want a clue to the likelihood of word possibilities

eg:

```java -jar wordlinsix.jar scholardle contains=oest not=mdlarnbikup not2=eo not3=s not4=e not5=s 6=t```

```java -jar wordlinsix.jar wordle contains=el not=thumpbownrga not2=le not5=l```


## Examples

Our first guess is the word **plumb**

![PLUMB](/assets/PLUMB.JPG?raw=true "Title")

We note that letters plum are not in the answer and letter b is in a position other than 5.

So we enter these parameters:

**java -jar wordlinsix.jar words=no  contains=b not=plum not5=b**

The app responds with...

*There are 125 word(s) matching words=no  contains=b not=plum not5=b*

So we make a second guess of word **beast**

![PLUMB_BEAST](/assets/PLUMB_BEAST.JPG?raw=true "Title")

We note that letters east are not in the answer and letter b is in a position other than 1

So we adjust the parameters:

**java -jar wordlinsix.jar words=no contains=b not=plumeast not1=b not5=b**

The app responds with...

*There are 3 word(s) matching words=no contains=b not=plumeast not1=b not5=b*

If we remove **words=no** or put **words=yes** the app will list the possible words

*There are 3 word(s) matching words=yes contains=b not=plumeast not1=b not5=b*

```
1       hobby
2       inbox
3       robin
```
By default **rank=yes** meaning the words will be sorted into a preference order based on letter distribution

*There are 3 word(s) matching words=yes rank=yes contains=b not=plumeast not1=b not5=b*
```
1       robin   2       0
2       inbox   2       0
3       hobby   2       1
```
The best choice will be the first on the list **robin**

![PLUMB_BEAST_ROBIN](/assets/PLUMB_BEAST_ROBIN.JPG?raw=true "Title")

Success!

## Example #2

Using the optimum starting word(s), the algorithm will converge on an answer suprisingly quickly.

![THUMP_BLOWN_DIRGE](/assets/THUMP_BLOWN_DIRGE.JPG?raw=true "Title")

```
>java -jar wordlinsix.jar rank=true words=yes contains=thore not=umpblwndig
There are 1 word(s) matching rank=true words=yes contains=thore not=umpblwndig
1       other   2       0
```
Maybe lucky, but even without specifying **not1=t not2=h not3=or not5=e** the algorithm was able to deduce there was one possibility:

![OTHER](/assets/OTHER.JPG?raw=true "Title")



## Automatic

The algorithm to solve a Wordle can be tested as follows:

**java -jar wordlinsix.jar guess1=thump answer=robin**

The app will look at the existing guesses and suggest the next guess.

The process will be repated until the answer is matched.

Obviously the app is told the answer, but the idea is the algorithm can be exercised to see how many guesses are required

```
>java -jar wordlinsix.jar guess1=thump answer=robin
guess1=thump
guess1=thump guess2=renal 1=r not3=n
guess1=thump guess2=renal guess3=robin contains=rn 1=r 2=o 3=b 4=i 5=n
Algorithm needed 3 guesses
```

There is no guarentee the algorithm will match the performance of a manual guess (see above: PLUMB/BEAST/ROBIN)


## debug=1

This is only used to establish the letter frequency for all the words in the xxxxx.txt dictionary stored in the jar
```
a=909
b=267
c=448
d=370
e=1056
f=207
g=300
h=379
i=647
j=27
k=202
l=648
m=298
n=550
o=673
p=346
q=29
r=837
s=618
t=667
u=457
v=149
w=194
x=37
y=417
z=35
```
The result should be stored in the xxxxx.properties resource file.

If there is ever a need to edit the xxxxx.txt file, then the corresponding property data should be regenerated using debug=1

This will maintain the correct letter frequency distribution used by the code


## debug=2

For the curious, the algorithm can be made to discover its optimum start word(s)

But it takes _**considerable**_ time. See *Technical Note* below

Use of the word *optimum* in this context does not mean the choice of words cannot be beaten: it just means that the Wordle will be solved in at most six guesses.

In the following debug log, the algorithm is looking for word combinations that result in 6 (0) 
meaning that at most, six guesses and zero failures (where a failure is considered taking > 6 guesses)

The output that is produced is this: [java -jar wordlinsix.jar wordle debug=2](/assets/wordle-debug=2.txt?raw=true "debug=2")


Corresponding data generated for the new scholardle wordlist

The output that is produced is this: [java -jar wordlinsix.jar scholardle debug=2](/assets/scholardle-debug=2.txt?raw=true "debug=2")


## debug=3

Best start words for **wordle**

```wordle debug=3```
or
```wordle debug=3 guess1=admin guess2=blown guess3=dirge```

```
#Tries	thump	blown	dirge
1	1	0	0
2	81	1	0
3	713	507	1
4	1014	1238	1641
5	407	509	603
6	91	58	70
7	8	2	0
===== ======= ======= ======= 
%PASS  99.997  99.999  100.000
FAIL     (8)     (2)     (0)
```

## New Scholardle with 6-letters
Using the new scholardle dictionary, all games can theoretically be solved in 7 or fewer tries

The list of words in the scholardle dictionary are _not_ common 6-letter words. 
Some are nonsense words, misspellings, abbreviations and proper nouns.

```scholardle debug=3```
or
```scholardle debug=3 guess1=biskup guess2=lenght guess3=warmed```

```
#Tries	biskup	lenght	warmed
1	1	0	0
2	222	1	0
3	3750	3277	1
4	6419	7120	9814
5	2064	2173	2759
6	304	218	223
7	43	19	14
8	8	3	0
===== ======= ======= ======= 
%PASS 99.9960 99.9983 99.9989
FAIL  (51)    (22)    (14)
```

Using the first two start words with dictionary scholardle shows that **22** games will fail to be completed in the required 6 or fewer tries

However, using the three start words all the games will be completed in 7 or fewer tries and just **14** games will fail (<0.001%)

Just FYI
```scholardle debug=3 guess1=biskup guess2=lenght guess3=warmed guess4=convoy guess5=sforza```

There is no advantage to having a 4th or 5th start word as the %PASS degrades after the 3rd 

```
#Tries	biskup	lenght	warmed	convoy	sforza
1	1	0	0	0	0
2	222	1	0	0	0
3	3750	3277	1	0	0
4	6419	7120	9814	1	0
5	2064	2173	2759	11949	1
6	304	218	223	838	12450
7	43	19	14	23	357
8	8	3	0	0	3
9	0	0	0	0	0
===== ======= ======= ======= ======= ======= 
%PASS 99.9960 99.9983 99.9989 99.9982 99.9719
FAIL  (51)    (22)    (14)    (23)   (360) 
```


## Old Scholardle with 5-letters

The list of words in the scholardle dictionary is more than five times larger than wordle's!

This means the game cannot always be solved in six tries, but theoretically it can be solved in 9.

Obviously this means that there will be games that will be lost, but by choosing the optimum starting word(s) the chances of a solution are drastically improved.
```five debug=3```
or
```five debug=3 guess1=frump guess2=thegn guess3=sloyd guess4=wacke```

```
#Tries	frump	thegn	sloyd	wacke
1	1	0	0	0
2	97	1	0	0
3	1506	1064	1	0
4	4493	4856	5191	1
5	3927	4553	5608	9729
6	1734	1691	1667	2842
7	685	521	348	342
8	309	185	109	48
9	135	66	43	11
10	54	26	6	0
11	23	10	0	0
12	9	0	0	0
13	0	0	0	0
===== ======= ======= ======= ======= 
%PASS  99.906  99.938  99.961  99.969
FAIL  (1215)   (808)   (506)   (401)
```

Using the selected start words with dictionary five shows that **401** games will fail to be completed in the required 6 or fewer tries

The preferred words may seem obscure, but can be used, in the specified order, to improve the chance of finding a solution.

These optimum words have been deduced using parameter **debug=2** with _**five**_ as the first parameter.

The output that is produced is this: [java -jar wordlinsix.jar five debug=2](/assets/five-debug=2.txt?raw=true "debug=2")

## Technical Note

On a very fast computer, debug=2 takes a long time to complete using _wordle_ as first parameter.

However using _scholardle_ as first parameter on a slow computer with **debug=2** will take much (_possibly **days** of_) computation time!

If one wishes to deduce again optimum words following changes to any of the wordle.txt|scholardle.txt|xxxxx.txt dictionaries

```java -jar wordlinsix.jar wordle debug=2 1>logA.txt 2>>&1``` or

```java -jar wordlinsix.jar scholardle debug=2 1>logA.txt 2>>&1```

In the event that the the command fails to complete for any reason, due to the length computation time, 
logA.txt will contain partial data only, but the program can be resumed using the previous output file as input:

```java -jar  wordlinsix.jar wordle|scholardle|xxxxx debug=2-logA.txt 1>logB.txt 2>>&1```

Ensure that the stdout & stderr are piped to a different file to the one used as input!
