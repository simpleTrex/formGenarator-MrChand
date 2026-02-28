//This class keeps all primitive model class object declarations

import { Time } from "@angular/common";

export const FIELD_TYPES = [
    {
        value: 'text',
        label: 'Text'
    },
    {
        value: 'textArea',
        label: 'Text Area'
    },
    {
        value: 'number',
        label: 'Number only'
    },
    {
        value: 'select',
        label: 'Select'
    },
    {
        value: 'radio',
        label: 'Radio'
    },
    {
        value: 'checkBox',
        label: 'Check Box'
    },
    {
        value: 'email',
        label: 'E-Mail'
    },
    {
        value: 'password',
        label: 'Password'
    },
    {
        value: 'date',
        label: 'Date'
    },
    {
        value: 'time',
        label: 'Time'
    },
    {
        value: 'datetime-local',
        label: 'Date-Time'
    },
    {
        value: 'color',
        label: 'Color plate'
    },
    {
        value: 'image',
        label: 'Image'
    },
    {//Used to load existing Business Object Models
        value: 'model',
        label: 'Custom Model'
    },
];

export class CustomRegularExpression {
    _id?: number;
    expression?: string;
    errorMessage?: string;
}

export class CustomOptions {
    _id?: number;
    optionLabel?: string;
    optionValue?: string;
}

export class FormField {
    _id?: number;
    name?: string;
    value?: string;
    options?: Array<CustomOptions> = new Array<CustomOptions>();
    model?: String = new String(); // Stores the model name
    modelData?: Array<CustomForm> = new Array<CustomForm>();//On rendering data form, load data for model type [selected data is saved in value]
    regularExpression?: Array<CustomRegularExpression> = new Array<CustomRegularExpression>();
    elementType?: string;
    placeHolder?: string;
}

class Notes {
    message?: string;
    timestamp?: Time;
}

//Domain object level BP action including permission roles
export class FormActionField {
    action: string;
    role: string;

    constructor(_a: string, _r: string) {
        this.action = _a;
        this.role = _r;
    }
}

export class Process{
    action: FormField = new FormField();
}

export class CustomForm {
    id?: number;
    name?: string;//TODO rename to class
    version?: number;
    fields?: Array<FormField> = new Array<FormField>();

    notes?: Array<Notes> = new Array<Notes>(); // User/System can add notes to this object in operation

    //This object can go through following business steps defined in the list. Current step is in @this.status
    //actions?: Map<number, FormActionField> = new Map<number, FormActionField>();
    process : Process = new Process();
    status: number = 0;
}
