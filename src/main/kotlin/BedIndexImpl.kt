/**
 * chrSizeMap saves the size of chromosomes
 * chrStartLineMap saves list of pair (start, N_line in document)
 */
class BedIndexImpl : BedIndex {
    private val chrSizeMap = HashMap<String, Int>()
    private val chrStartLineMap = HashMap<String, MutableList<Pair<Int, Int>>>()

    /**
     * Add in dictionaries
     */
    override fun add(chr: String, size: Int, startLines: MutableList<Pair<Int, Int>>) {
        if (!chrStartLineMap.containsKey(chr))
            chrStartLineMap[chr] = mutableListOf()
        if (!chrSizeMap.containsKey(chr))
            chrSizeMap[chr] = size
        chrStartLineMap[chr]?.addAll(startLines)
    }

    override fun getStartLines(chr: String): MutableList<Pair<Int, Int>>? {
        return chrStartLineMap[chr]
    }

    override fun getSizeChr(chr: String): Int? {
        return chrSizeMap[chr]
    }
}