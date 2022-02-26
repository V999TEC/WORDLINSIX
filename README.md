# WORDLINSIX
Java CLI to solve any WORDLE puzzle in a maximum of six tries (typically 4)


### >java -jar wordlinsix.jar (parameters)

## Parameter Help:
        Use words=yes or words=true to show possible words
        Use rank=yes or rank=true to rank each word based on letter probability
        Use 1=b to indicate first letter is definitely b
        Eliminate letters by using not=abcdefg etc.
        Use 5=z to indicate last letter is definitely z
        Use 2=j 3=k  to indicate letters 'jk' are found between positions 2 and 3
        Use 3=j 4=k  to indicate letters 'jk' are found between positions 3 and 4
        Use contains=iou to indicate letters i and o and u *must* appear in the word
        Use not2=ab to indicate second letter cannot be a or b
        Use not5=y to indicate fifth letter cannot be y
        


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

If we change **words=yes** the app will list the possible words

*There are 3 word(s) matching words=yes contains=b not=plumeast not1=b not5=b*

```
1       hobby
2       inbox
3       robin
```
But if we add **rank=yes** the words will be sorted into a preference order based on letter distribution

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

Using the optimum three starting words, the algorithm will converge on an answer suprisingly quickly.

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

### guess1=thump

This will solve 99.8% of Wordles with a mean number of guesses of 3.8

### guess1=thump guess2=blown

This will solve 99.91% of Wordles with a mean number of guesses of 4.0

### guess1=thump guess2=blown guess3=dirge

This will solve 100% of Wordles with a mean number of guesses of 4.2

## debug=1

This is only used to establish the letter frequency for all the words in the Wordle dictionary
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
The result should be stored in the frequencies.properties resource file.

If the words.txt file changes then the corresponding property data should be regenerated using debug=1


## debug=2

For the curious, the algorithm can be made to discover its optimum three start word(s)

But it takes considerable time.

Use of the word *optimum* in this context does not mean the choice of words cannot be beaten: it just means that the Wordle will be solved in at most six guesses.

In the following debug log, the algorithm is looking for word combinations that result in 6 (0) 
meaning that at most, six guesses and zero failures (where a failure is considered taking > 6 guesses)

[DEBUG=2](/assets/debug=2.txt?raw=true "debug=2")

The log shows that initially we discover
```abase	9 (29)  <----```
then
```blend	8 (11)  <----```
then
```thump	7 (4)  <----```

Using **thump** as the first word the algorithm then tries all the second words an so on, looking for a score of 6 (0) or better

As far as this algorithm is concerned, the worst possible start word is: ```queue	11 (90)``` meaning there were 90 games that could not be completed in 6 guesses and some of those needed 11 guesses!



## debug=3

```
Tries	thump +	blown +	dirge
1	1	0	0
2	81	1	0
3	713	507	1
4	1019	1238	1641
5	405	509	603
6	92	58	70
7	4	2	0
```
What this table is showing is:

### Use THUMP as the first word 
then 81 answers will be correct on the 2nd guess
but 92 answers will need 6 guesses and 4 answers will be unsuccessful


### Use THUMP as the first word **and** BLOWN as the second word
then 507 answers will be correct on the 3rd guess
but 58 answers will need 6 guesses and 2 answers will be unsuccessful


### Use THUMP as the first word **and** BLOWN as the second word **and** DIRGE as the third word
then 1641 answers will be correct on the 4th guess
but 70 answers will need 6 guesses but none will need more than 6
