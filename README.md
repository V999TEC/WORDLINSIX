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

This will solve 97% of Wordles with a mean number of guesses of 3.8

### guess1=thump guess2=blown

This will solve 98.5% of Wordles with a mean number of guesses of 4.0

### guess1=thump guess2=blown guess3=dirge

This will solve 100% of Wordles with a mean number of guesses of 4.2

## Examples
![Alt text](relative/path/to/img.jpg?raw=true "Title")

