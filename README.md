    gvgai
=====

Note: The Learning track code is not in this repository, but here: https://github.com/rubenrtorrado/GVGAI_GYM


This is the framework for the General Video Game Competition, used for the Planning and PCG Tracks - http://www.gvgai.net/

Google group - https://groups.google.com/forum/#!forum/the-general-video-game-competition

## FAQs / Troubleshooting

**3. Where are the Test methods? Due to the explosion of GVGAI competition tracks, we have distributed the main methods in different classes across the code hierarchy:

 - Single Player Planning track: tracks.singlePlayer.Test.java
 - 2-Player Planning track: tracks.multiPlayer.TestMultiPlayer.java
 - Level Generation track: tracks.levelGeneration.TestLevelGeneration.java
 - Rule Generation track: tracks.ruleGeneration.TestRuleGeneration.java


**2. How do I upload my controller? What files or folder structure do I need? 
First of all, your controller ```Agent.java``` and any auxiliary files you create should be in a single package folder with your username. For example, if your username is "abc", you should have a package folder named "abc" in the project. Your entire project layout should look something like this:

```groovy
- abc
	|- Agent.java
	|- MyAdditionalFile1.java
	|- MyAdditionalFile2.java
- tracks
- core
- ontology
- tools
```

Then, all you need to do is to zip and upload the "abc" folder. No other folders/files are necessary.


**3. I am getting the error `javac1.8 class not found` when running Eclipse and ANT on build.xml**
This is likely because the ANT version that is installed with your version of Eclipse is old. You can easily fix this problem by doing the following:

- Download the archive of the [latest version of ANT](http://ant.apache.org/bindownload.cgi) (Tested with  Ant 1.9.4)
- Extract the archive onto a local folder on your computer (e.g., /Users/gvgai/ant/apache-ant-1.9.4/)
- In Eclipse, go to Eclipse -> Preferences -> Ant -> Runtime
- Click on "Ant Home'' button on the right.
- Select the folder, which you extracted ANT into (e.g., /Users/gvgai/ant/apache-ant-1.9.4/)

## Measures table:

| Algorithm  | Map      | Runtime (ms) | Path length   | Expanded Nodes   |
|------------|----------|--------------|---------------|------------------|
|            | Small    | 0.2461       | 36            | 89               |
| Dijkstra   | Medium   | 1.8710       | 114           | 566              |
|            | Big      | 2.6818       | 808           | 2145             |
| ---------  | -------- | ------------ | ------------  | ---------------- |
|            | Small    | 0.9225       | 36            | 84               |
| A*         | Medium   | 3.9987       | 114           | 557              |
|            | Big      | 4.4909       | 808           | 2128             |
| ---------  | -------- | ------------ | ------------  | ---------------- |
|            | Small    | 5.1190       | 40            | 41               |
| RTA*       | Medium   | 58.3568      | 338           | 339              |
|            | Big      | 690.9635     | 3386          | 3387             |
| ---------  | -------- | ------------ | ------------  | ---------------- |
|            | Small    | 7.2216       | 60            | 61               |
| LRTA*      | Medium   | 416.9904     | 5658          | 5659             |
|            | Big      | TE           | TE            | TE               |
| ---------  | -------- | ------------ | ------------  | ---------------- |
| EXTENDED   | EXTENDED | EXTENDED     | EXTENDED      | EXTENDED         |
| ---------  | -------- | ------------ | ------------  | ---------------- |
|            | Small    | 12.2061      | 85            | 86               |
| RTA*       | Medium   | 51.9014      | 338           | 339              |
|            | Big      | 693.9086     | 3402          | 3403             |
| ---------  | -------- | ------------ | ------------  | ---------------- |
|            | Small    | 15.9032      | 151           | 152              |
| LRTA*      | Medium   | 428.3779     | 5658          | 5659             |
|            | Big      | TE           | TE            | TE               |
| ---------  | -------- | ------------ | ------------  | ---------------- |


#### TODO
- A*: instead of using two boolean matrices, use one byte-value matrix, to leverage locality principle in cache.
- A* and Dijkstra: change the way of incrementing nodes expanded by incrementing when it checks if current node is goal.

# References
How to create custom `hashCode` and `equals` methods:
  - https://stackoverflow.com/questions/27581/what-issues-should-be-considered-when-overriding-equals-and-hashcode-in-java/27609#27609
  - https://www.youtube.com/watch?v=9x-h3X8MHvg
