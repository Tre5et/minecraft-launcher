"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
        desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.convert = void 0;
var console = __importStar(require("console"));
var Update_1 = require("./Update");
var fs = require('fs');
var input = "./version_manifests";
var output = "./update";
function convert() {
    var files = fs.readdirSync(input, { withFileTypes: true });
    var updates = [];
    for (var _i = 0, files_1 = files; _i < files_1.length; _i++) {
        var file = files_1[_i];
        var content = fs.readFileSync(input + "/" + file.name, 'utf8');
        updates.push(JSON.parse(content));
    }
    if (updates.length == 0) {
        return;
    }
    console.log("creating latest file " + updates[updates.length - 1].version);
    var latestUpdate = new Update_1.Update(false);
    console.log(latestUpdate);
    fs.writeFileSync(output + "/" + updates[updates.length - 1].version, JSON.stringify(latestUpdate));
    if (updates.length < 2) {
        return;
    }
    for (var i = updates.length - 2; i >= 0; i--) {
        var creating = updates[i];
        if (!creating.version) {
            return;
        }
        console.log("creating file " + creating.version);
        var new_version = undefined;
        var updater_url = undefined;
        var files_3 = [];
        for (var j = i + 1; j < updates.length; j++) {
            var current = updates[j];
            if (!current.version || (current.requires && !in_requires(creating.version, current.requires))) {
                continue;
            }
            new_version = current.version;
            if (current.updaterUrl) {
                updater_url = current.updaterUrl;
            }
            if (!current.files) {
                continue;
            }
            var currentFiles = current.files;
            for (var _a = 0, currentFiles_1 = currentFiles; _a < currentFiles_1.length; _a++) {
                var currentFile = currentFiles_1[_a];
                var found = false;
                for (var _b = 0, files_2 = files_3; _b < files_2.length; _b++) {
                    var file = files_2[_b];
                    if (file.update && currentFile.remove && file.update == currentFile.remove) {
                        found = true;
                        file.update = currentFile.update;
                        file.url = currentFile.url;
                        break;
                    }
                }
                if (!found) {
                    files_3.push(currentFile);
                }
            }
        }
        var update = new Update_1.Update(new_version != undefined, new_version, undefined, new_version ? files_3 : undefined, new_version ? updater_url : undefined);
        console.log(update);
        fs.writeFileSync(output + "/" + updates[i].version, JSON.stringify(update));
    }
}
exports.convert = convert;
function in_requires(version, requires) {
    var operator = requires.substring(0, 1);
    var requires_parts = requires.substring(1).split(".");
    var version_parts = version.split(".");
    if (operator == "=") {
        for (var i = 0; i < requires_parts.length; i++) {
            if (requires_parts[i] == "*") {
                return true;
            }
            if (Number(requires_parts[i]) != Number(version_parts[i])) {
                return false;
            }
        }
        return true;
    }
    if (operator == ">") {
        for (var i = 0; i < requires_parts.length; i++) {
            if (Number(version_parts[i]) < Number(requires_parts[i])) {
                return false;
            }
            if (Number(version_parts[i]) > Number(requires_parts[i])) {
                return true;
            }
        }
        return Number(version_parts[requires_parts.length - 1]) > Number(requires_parts[requires_parts.length - 1]);
    }
    if (operator == "<") {
        for (var i = 0; i < requires_parts.length; i++) {
            if (Number(version_parts[i]) > Number(requires_parts[i])) {
                return false;
            }
            if (Number(version_parts[i]) < Number(requires_parts[i])) {
                return true;
            }
        }
        return Number(version_parts[requires_parts.length - 1]) < Number(requires_parts[requires_parts.length - 1]);
    }
    return false;
}
convert();
//# sourceMappingURL=index.js.map