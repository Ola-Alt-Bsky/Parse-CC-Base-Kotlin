package com.laoluade

import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.util.*

fun parse_to_json(file_lines: MutableList<String>): JSONObject {
    val info: JSONObject = JSONObject()

    var last_season: String? = null
    var last_episode: String? = null
    var last_attribute: String? = null
    var last_content: String? = null
    var last_specific: String? = null

    for (line in file_lines) {
        val starts_with_star: Boolean = line.startsWith("*")
        val starts_with_space: Boolean = line.startsWith(" ")
        val amount_leading_space: Int = line.length - line.trimStart().length

        when {
            // Season
            !(starts_with_star || starts_with_space) -> {
                val trimmed_line: String = line.replaceFirst("^\uFEFF", "")
                info.put(trimmed_line, JSONObject())
                last_season = trimmed_line
            }
            // Episode
            starts_with_star -> {
                val trimmed_line: String = line.replace('*', ' ').trim()
                val cur_season: JSONObject = info.getJSONObject(last_season)
                cur_season.put(trimmed_line, JSONObject())
                last_episode = trimmed_line
            }
            // Attribute
            starts_with_space && amount_leading_space == 3 -> {
                val trimmed_line: String = line.replace('*', ' ').trim()
                val cur_season: JSONObject = info.getJSONObject(last_season)
                val cur_episode: JSONObject = cur_season.getJSONObject(last_episode)

                val if_songs: (String) -> Any = { attr: String -> when (attr) {
                    "Songs" -> JSONObject()
                    else -> mutableListOf<String>()
                }}
                cur_episode.put(trimmed_line, if_songs(trimmed_line))
                last_attribute = trimmed_line
            }
            // Content
            starts_with_space && amount_leading_space == 6 -> {
                val trimmed_line: String = line.replace('*', ' ').trim()
                val cur_season: JSONObject = info.getJSONObject(last_season)
                val cur_episode: JSONObject = cur_season.getJSONObject(last_episode)

                assert(last_attribute != null)

                when (last_attribute) {
                    "Songs" -> cur_episode.getJSONObject(last_attribute).put(trimmed_line, JSONObject())
                    else -> {
                        val cur_array = cur_episode.get(last_attribute) as MutableList<String>
                        cur_array.add(trimmed_line)
                    }
                }

                last_content = trimmed_line
            }
            // Specific
            starts_with_space && amount_leading_space == 9 -> {
                val trimmed_line: String = line.replace('*', ' ').trim()
                val cur_season: JSONObject = info.getJSONObject(last_season)
                val cur_episode: JSONObject = cur_season.getJSONObject(last_episode)
                val cur_attribute: JSONObject = cur_episode.getJSONObject(last_attribute)
                val cur_content: JSONObject = cur_attribute.getJSONObject(last_content)

                assert(last_content != null)

                when (last_content) {
                    "Scene Specific" -> cur_content.put(trimmed_line,  JSONObject())
                    else -> cur_attribute.put(last_content, trimmed_line)
                }

                last_specific = trimmed_line
            }
            // Scene Specific
            starts_with_space && amount_leading_space == 12 -> {
                val trimmed_line: String = line.replace('*', ' ').trim()
                val cur_season: JSONObject = info.getJSONObject(last_season)
                val cur_episode: JSONObject = cur_season.getJSONObject(last_episode)
                val cur_attribute: JSONObject = cur_episode.getJSONObject(last_attribute)
                val cur_content: JSONObject = cur_attribute.getJSONObject(last_content)
                cur_content.put(last_specific, trimmed_line)
            }
            // Default Case
            else -> {
                println("Edge Case")
            }
        }
    }

    // Remove extra stuff
    info.remove("Chapter Template")
    info.remove("Extra Songs")

    return info
}

fun get_items(info: JSONObject, item: Int): Set<String> {
    // 1 for characters, 2 for locations, 3 for songs

    val item_set: MutableSet<String> = mutableSetOf<String>()
    val season_list: Set<String> = info.keySet()

    for (season_name: String in season_list) {
        val season: JSONObject = info.getJSONObject(season_name)
        val episode_list: Set<String> = season.keySet()

        for (episode_name: String in episode_list) {
            val episode: JSONObject = season.getJSONObject(episode_name)

            when (item) {
                // Characters
                1 -> {
                    val ep_items: MutableList<String> = episode.get("Characters") as MutableList<String>
                    for (ep_item: String in ep_items) {
                        item_set.add(ep_item)
                    }
                }
                // Locations
                2 -> {
                    val ep_items: MutableList<String> = episode.get("Locations") as MutableList<String>
                    for (ep_item: String in ep_items) {
                        item_set.add(ep_item)
                    }
                }
                // Songs
                3 -> {
                    item_set.add(episode.getJSONObject("Songs").get("Intro Song") as String)
                    item_set.add(episode.getJSONObject("Songs").get("Outro Song") as String)

                    val ep_scenes: JSONObject = episode.getJSONObject("Songs").getJSONObject("Scene Specific")
                    val ep_scene_names: Set<String> = ep_scenes.keySet()

                    for (cur_scene: String in ep_scene_names) {
                        item_set.add(ep_scenes.get(cur_scene) as String)
                    }
                }
            }
        }
    }

    return item_set
}

fun main() {
    // Read input from a .txt file
    val input: Scanner = Scanner(System.`in`)
    println("Welcome! You will need to enter in the location of your file.")
    print("Enter in the ABSOLUTE file path of the base txt file: ")
    var file_path: String = input.nextLine()
    file_path = file_path.replace('"', ' ').trim()

    val file_lines: MutableList<String> = mutableListOf();

    // Try to retrieve the text from the file
    try {
        val myObj: File = File(file_path)
        val myReader: Scanner = Scanner(myObj)
        while (myReader.hasNextLine()) {
            var data: String = myReader.nextLine()
            file_lines.add(data)
        }
        myReader.close()
    } catch (e: FileNotFoundException) {
        println("An error occurred. The file was not found.")
    }

    // Parse and convert to JSON
    val parse_json: JSONObject = parse_to_json(file_lines)

    // Retrieve a list of characters, locations, and songs
    val characters: Set<String> = get_items(parse_json, 1)
    val locations: Set<String> = get_items(parse_json, 2)
    val songs: Set<String> = get_items(parse_json, 3)
    
    // Save the parsed JSON information to a folder
    val parent_dir = File(file_path).getParent()
    val output_dir = "$parent_dir/Output"
    val output_name = "Casual_Roleplay"
    val output_path = "$output_dir/$output_name"

    val dir_created: Boolean = when {
        !File(output_dir).exists() -> File(output_dir).mkdir()
        else -> true
    }

    if (dir_created) {
        val fwriter: FileWriter = FileWriter("$output_path.json")
        fwriter.write(parse_json.toString(4))
        fwriter.close()
        println("Parsed JSON has been saved to $output_path.json.")

        val save_set = {name: String, fp: String, items: Set<String> ->
            val bwriter: BufferedWriter = BufferedWriter(FileWriter(fp))
            for (item: String in items) {
                bwriter.write(item)
                bwriter.newLine()
            }
            bwriter.close()
            println("$name have been saved to $fp.")
        }

        save_set("Characters", output_path + "_characters.txt", characters)
        save_set("Locations", output_path + "_locations.txt", locations)
        save_set("Songs", output_path + "_songs.txt", songs)
    }
}
