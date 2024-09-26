import {WorkFlowy} from 'workflowy'
import {program} from 'commander'
import express from 'express'
import * as fs from 'fs';


function deep_find_by_id(item, id) {
    function deepFindById_(item, id, result) {
        if (item.id.endsWith(id)) {
            result.item = item
        }
        for (const candidate of item.items) {
            deepFindById_(candidate, id, result);
        }
    }

    let result = {}
    deepFindById_(item, id, result)
    return result.item
}

function update_item(options, item) {
    item //
        .setName(options.name) //
        .setNote(options.note);
}

const Results = Object.freeze({
    CREATED: Symbol("created"), //
    UPDATED: Symbol("updated"), //
    INBOX_BULLET_NOT_FOUND: Symbol("inbox_bullet_not_found")
});

function to_result(id, result, error) {
    return {id, result: result.description, error}
}

async function update(options, document) {
    const rootList = document.root;

    const parentId = options.config.newItemParentId;
    const inboxBullet = deep_find_by_id(rootList, parentId);
    if (!inboxBullet) {
        return to_result(null, Results.INBOX_BULLET_NOT_FOUND)
    }

    if (options.itemId) {
        const result = deep_find_by_id(rootList, options.itemId);
        if (result) {
            return update_bullet(options, result)
        } else {
            return create_bullet(options, inboxBullet)
        }
    } else {
        return create_bullet(options, inboxBullet)
    }
}

async function create_bullet(options, inboxBullet) {
    const newItem = inboxBullet.createItem();
    update_item(options, newItem)
    return to_result(newItem.id, Results.CREATED)
}

async function update_bullet(options, currentBullet) {
    currentBullet //
        .setName(options.name) //
        .setNote(options.note)
    return to_result(currentBullet.id, Results.UPDATED)
}


function read_config(configFile) {
    const json = fs.readFileSync(configFile, 'utf8')
    return JSON.parse(json).entries;
}

async function update_and_save(options, document) {
    const result = await update(options, document);
    if (document.isDirty()) {
        await document.save();
    }
    return result
}

program.requiredOption('--port <number>')
program.parse();

const program_options = program.opts()
const port = program_options.port
let workflowy;
const app = express()

app.get('/update', (req, res) => {
    const configFileParam = req.query.configFile;
    const itemIdParam = req.query.itemId;
    const nameParam = req.query.name;
    const noteParam = req.query.note;

    const completeConfig = read_config(configFileParam)
    const options = {
        name: nameParam, //
        note: noteParam, //
        config: {
            newItemParentId: completeConfig.WORKFLOWY_NEW_ITEM_PARENT_ID,
            workflowyUsername: completeConfig.WORKFLOWY_USERNAME,
            workflowyPassword: completeConfig.WORKFLOWY_PASSWORD

        },
        itemId: itemIdParam
    }
    res.setHeader('Content-Type', 'application/json');
    if (!workflowy) {
        workflowy = new WorkFlowy(options.config.workflowyUsername, options.config.workflowyPassword);
    }
    workflowy.getDocument() //
        .then(document => update_and_save(options, document)) //
        .then(result => res.send(JSON.stringify(result)))
})

app.listen(port, () => {
    console.log(`Listening on port ${port}`)
})
