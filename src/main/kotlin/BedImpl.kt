import java.nio.file.Path

class BedImpl : BedReader {

    /**
     * break to tokens of line and
     * send to dictionaries
     */
    private fun tokenizer(
        line: String,
        numberOfLine: Int,
        dict: HashMap<String, MutableList<Pair<Int, Int>>>,
        sizeChrMap: HashMap<String, Int>
    ):
            HashMap<String, MutableList<Pair<Int, Int>>> {
        val chrStartEndList = line.split(" ")
        val chrName = chrStartEndList[0]
        val start = chrStartEndList[1].toInt()
        val end = chrStartEndList[2].toInt()

        if (!dict.containsKey(chrName)) {
            dict[chrName] = mutableListOf(Pair(start, numberOfLine))
        } else {
            dict[chrName]?.add(Pair(start, numberOfLine))
        }

        if (!sizeChrMap.containsKey(chrName)) {
            val size = end - start
            sizeChrMap[chrName] = size
        }
        return dict
    }

    /**
     * Sort by start for binary search in feature
     */
    private fun sortByStart(dict: HashMap<String, MutableList<Pair<Int, Int>>>) {
        for (startLineList in dict.values) {
            startLineList.sortBy { it.first }
        }
    }

    /**
     * Creates index for [bedPath] and saves it to [indexPath]
     * chromocome size (start N_line)^N
     */
    override fun createIndex(bedPath: Path, indexPath: Path) {
        val inputFile = bedPath.toFile()
        var numberOfLine = 0
        var dict = HashMap<String, MutableList<Pair<Int, Int>>>()
        val chrSizeMap = HashMap<String, Int>()
        inputFile.useLines { lines ->
            lines.forEach { line ->
                dict = tokenizer(line, numberOfLine, dict, chrSizeMap)
                numberOfLine += 1
            }
        }
        sortByStart(dict)
        val outputFile = indexPath.toFile()
        for (entry in dict.entries) {
            outputFile.writeText("${entry.key} ${chrSizeMap[entry.key]} ")
            for (el in entry.value) {
                outputFile.writeText("${el.first} ${el.second}\n")
            }
        }

    }

    /**
     * Load file line to dictionaries
     * File consists of
     * chrName size start size (start size)*
     */
    private fun loader(line: String, bedIndex: BedIndexImpl) {
        val chrSizePair = line.split(" ")
        val chrName = chrSizePair[0]
        val size = chrSizePair[1].toInt()

        val startLines = mutableListOf<Pair<Int, Int>>()
        for (i in 2..chrSizePair.size step 2) {
            val start = chrSizePair[i].toInt()
            val lineOfNumber = chrSizePair[i + 1].toInt()
            startLines.add(Pair(start, lineOfNumber))
        }

        bedIndex.add(chrName, size, startLines)
    }

    /**
     * Loads [BedIndex] instance from file [indexPath]
     */
    override fun loadIndex(indexPath: Path): BedIndex {
        val inputFile = indexPath.toFile()
        val bedIndex = BedIndexImpl()
        inputFile.useLines { lines ->
            lines.forEach { line ->
                loader(line, bedIndex)
            }
        }
        return bedIndex
    }

    /**
     * Loads list of [BedEntry] from file [bedPath] using [index].
     * All the loaded entries should be located on the given [chromosome],
     * and be inside the range from [start] inclusive to [end] exclusive.
     * E.g. entry [1, 2) is inside [0, 2), but not inside [0, 1).
     */
    override fun findWithIndex(
        index: BedIndex,
        bedPath: Path,
        chromosome: String,
        start: Int,
        end: Int
    ): List<BedEntry> {
        val startLines = index.getStartLines(chromosome) ?: mutableListOf()
        val sizeChr = index.getSizeChr(chromosome) ?: 0
        val searchL = binarySearch(startLines, start)
        val searchR = binarySearch(startLines, end - sizeChr)

        val linesDoc = mutableListOf<Int>()
        for (i in searchL.second..searchR.first) {
            linesDoc.add(startLines[i].second)
        }
        linesDoc.sort()

        val bedEntry = mutableListOf<BedEntry>()
        val inputFile = bedPath.toFile()
        var i = 0
        var curLine = 0
        inputFile.useLines { lines ->
            lines.forEach { line ->
                if (curLine == linesDoc[i]) {
                    val tokens = line.split(" ")
                    bedEntry.add(
                        BedEntry(
                            tokens[0],
                            tokens[1].toInt(),
                            tokens[2].toInt(),
                            tokens.subList(3, tokens.size - 1)
                        )
                    )
                    i += 1
                }
                curLine += 1
            }
        }
        return bedEntry
    }
}

/**
 * Binary search
 */
fun binarySearch(arr: MutableList<Pair<Int, Int>>, start: Int): Pair<Int, Int> {
    var l = 0
    var r = arr.size
    while (r - l > 1) {
        val mid = l + (r - l) / 2

        if (arr[mid].first >= start)
            r = mid
        else
            l = mid
    }
    return Pair(l, r)
}