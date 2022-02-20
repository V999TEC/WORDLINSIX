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
        
### guess1=thump

This will solve 99.8% of Wordles with a mean number of guesses of 3.8

### guess1=thump guess2=blown

This will solve 99.91% of Wordles with a mean number of guesses of 4.0

### guess1=thump guess2=blown guess3=dirge

This will solve 100% of Wordles with a mean number of guesses of 4.2

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




