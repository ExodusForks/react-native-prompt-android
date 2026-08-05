import {
    NativeModules
} from 'react-native';
const PromptAndroid = NativeModules.PromptAndroid;

export type PromptType = $Enum<{
    /**
     * Default alert with no inputs
     */
        'default': string,
    /**
     * Plain text input alert
     */
        'plain-text': string,
    /**
     * Secure text input alert
     */
        'secure-text': string,
    /**
     * Numeric input alert
     */
        'numeric': string,
    /**
     * Email address input alert
     */
        'email-address': string,
    /**
     * Phone pad input alert
     */
        'phone-pad': string,
}>;

export type PromptStyle = $Enum<{
    /**
     * Default alert dialog style
     */
    'default': string,
    /**
     * Shimo alert dialog style
     */
    'shimo': string,
}>;

type PromptOptions = {
    cancelable?: ?boolean;
    type?: ?PromptType;
    defaultValue?: ?String;
    placeholder?: ?String;
    style?: ?PromptStyle;
};

type AlertOptions = {
    cancelable?: ?boolean,
    userInterfaceStyle?: 'unspecified' | 'light' | 'dark',
    onDismiss?: ?() => void,
    showOverTopActivity?: ?boolean,
};

/**
 * Array or buttons
 * @typedef {Array} ButtonsArray
 * @property {string=} text Button label
 * @property {Function=} onPress Callback function when button pressed
 */
type ButtonsArray = Array<{
    /**
     * Button label
     */
        text?: string,
    /**
     * Callback function when button pressed
     */
        onPress?: ?Function,
}>;

type AlertButtonStyle = 'default' | 'cancel' | 'destructive';

type Buttons = Array<{
    text?: string,
    onPress?: ?Function,
    isPreferred?: boolean,
    style?: AlertButtonStyle,
}>;

type DialogOptions = {
    title?: string,
    message?: string,
    buttonPositive?: string,
    buttonNegative?: string,
    buttonNeutral?: string,
    items?: Array<string>,
    cancelable?: boolean,
    showOverTopActivity?: boolean,
};

/**
 * reference: https://github.com/facebook/react-native/blob/e71b094b24ea5f135308b1e66c86216d9d693403/Libraries/Alert/Alert.js#L43-L114
 */
function alert(
    title: ?string,
    message?: ?string,
    buttons?: Buttons,
    options?: AlertOptions,
): void {
    
    const config: DialogOptions = {
        title: title || '',
        message: message || '',
        cancelable: false,
    };

    if (options && options.cancelable) {
        config.cancelable = options.cancelable;
    }
    if (options && options.showOverTopActivity) {
        config.showOverTopActivity = true;
    }
    // At most three buttons (neutral, negative, positive). Ignore rest.
    // The text 'OK' should be probably localized. iOS Alert does that in native.
    const defaultPositiveText = 'OK';
    const validButtons: Buttons = (buttons && buttons.length > 0)
        ? buttons.slice(0, 3)
        : [{text: defaultPositiveText}];
    const buttonPositive = validButtons.pop();
    const buttonNegative = validButtons.pop();
    const buttonNeutral = validButtons.pop();

    if (buttonNeutral) {
        config.buttonNeutral = buttonNeutral.text || '';
    }
    if (buttonNegative) {
        config.buttonNegative = buttonNegative.text || '';
    }
    if (buttonPositive) {
        config.buttonPositive = buttonPositive.text || defaultPositiveText;
    }

    /* $FlowFixMe[missing-local-annot] The type annotation(s) required by
    * Flow's LTI update could not be added via codemod */
    const onAction = (action, buttonKey) => {
        if (action === PromptAndroid.buttonClicked) {
            if (buttonKey === PromptAndroid.buttonNeutral) {
                buttonNeutral.onPress && buttonNeutral.onPress();
            } else if (buttonKey === PromptAndroid.buttonNegative) {
                buttonNegative.onPress && buttonNegative.onPress();
            } else if (buttonKey === PromptAndroid.buttonPositive) {
                buttonPositive.onPress && buttonPositive.onPress();
            }
        } else if (action === PromptAndroid.dismissed) {
            options && options.onDismiss && options.onDismiss();
        }
    };

    PromptAndroid.alertWithArgs(config, onAction);
}

function prompt(
    title: ?string,
    message?: ?string,
    callbackOrButtons?: ?((text: string) => void) | ButtonsArray,
    options?: PromptOptions
): void {
    const defaultButtons = [
      {
        text: 'Cancel',
      },
      {
        text: 'OK',
        onPress: callbackOrButtons
      }
    ];

    let buttons = typeof callbackOrButtons === 'function'
      ? defaultButtons
      : callbackOrButtons;
      
    let config = {
        title: title || '',
        message: message || '',
    };

    if (options) {
        config = {
            ...config,
            cancelable: options.cancelable !== false,
            type: options.type || 'default',
            style: options.style || 'default',
            defaultValue: options.defaultValue || '',
            placeholder: options.placeholder || ''
        };
    }
    // At most three buttons (neutral, negative, positive). Ignore rest.
    // The text 'OK' should be probably localized. iOS Alert does that in native.
    const validButtons: Buttons = buttons ? buttons.slice(0, 3) : [{text: 'OK'}];
    const buttonPositive = validButtons.pop();
    const buttonNegative = validButtons.pop();
    const buttonNeutral = validButtons.pop();

    if (buttonNeutral) {
        config = {...config, buttonNeutral: buttonNeutral.text || '' };
    }
    if (buttonNegative) {
        config = {...config, buttonNegative: buttonNegative.text || '' };
    }
    if (buttonPositive) {
        config = {
            ...config,
            buttonPositive: buttonPositive.text || ''
        };
    }


    PromptAndroid.promptWithArgs(
        config,
        (action, buttonKey, input) => {
            if (action !== PromptAndroid.buttonClicked) {
                return;
            }
            if (buttonKey === PromptAndroid.buttonNeutral) {
                buttonNeutral.onPress && buttonNeutral.onPress(input);
            } else if (buttonKey === PromptAndroid.buttonNegative) {
                buttonNegative.onPress && buttonNegative.onPress();
            } else if (buttonKey === PromptAndroid.buttonPositive) {
                buttonPositive.onPress && buttonPositive.onPress(input);
            }
        }
    );
}

export default {
    alert,
    prompt
};
